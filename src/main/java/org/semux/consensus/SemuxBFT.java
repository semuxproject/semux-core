/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.semux.Config;
import org.semux.consensus.Proposal.Proof;
import org.semux.consensus.SemuxBFT.Event.Type;
import org.semux.core.Account;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.Consensus;
import org.semux.core.Delegate;
import org.semux.core.PendingManager;
import org.semux.core.Sync;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.EdDSA;
import org.semux.crypto.EdDSA.Signature;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BFTNewHeightMessage;
import org.semux.net.msg.consensus.BFTProposalMessage;
import org.semux.net.msg.consensus.BFTVoteMessage;
import org.semux.utils.ArrayUtil;
import org.semux.utils.Bytes;
import org.semux.utils.MerkleTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemuxBFT implements Consensus {
    static final Logger logger = LoggerFactory.getLogger(SemuxBFT.class);

    private Blockchain chain;
    private ChannelManager channelMgr;
    private PendingManager pendingMgr;
    private Sync sync;

    private EdDSA coinbase;

    private AccountState accountState;
    private DelegateState delegateState;

    private Timer timer;
    private Broadcaster broadcaster;
    private BlockingQueue<Event> events;

    private Status status;
    private State state;

    private Block prevBlock;

    private long height;
    private int view;
    private volatile List<String> validators;
    private volatile List<Channel> activeValidators;
    private boolean committed;

    private Proposal proposal;
    private VoteSet proposalVotes;
    private VoteSet precommitVotes;
    private VoteSet commitVotes;

    private static SemuxBFT instance;

    /**
     * Get the singleton instance of consensus.
     * 
     * @return
     */
    public static synchronized SemuxBFT getInstance() {
        if (instance == null) {
            instance = new SemuxBFT();
        }

        return instance;
    }

    private SemuxBFT() {
    }

    @Override
    public void init(Blockchain chain, ChannelManager channelMgr, PendingManager pendingMgr, EdDSA coinbase) {
        this.chain = chain;
        this.channelMgr = channelMgr;
        this.pendingMgr = pendingMgr;
        this.coinbase = coinbase;

        this.accountState = chain.getAccountState();
        this.delegateState = chain.getDeleteState();

        this.sync = SemuxSync.getInstance();
        this.sync.init(chain, channelMgr);
        this.timer = new Timer();
        this.broadcaster = new Broadcaster();
        this.events = new LinkedBlockingQueue<>();

        this.status = Status.STOPPED;
        this.state = State.NEW_HEIGHT;
    }

    /**
     * Pause the consensus, and do synchronization.
     * 
     * @return true if sync finished normally; false if consensus is not in running
     *         status or stop was requested when synchronizing.
     */
    private boolean sync(long target) {
        if (status == Status.RUNNING) {
            status = Status.SYNCING;

            sync.start(target);

            if (status != Status.STOPPED) {
                status = Status.RUNNING;
                return true;
            }
        }

        return false;
    }

    /**
     * Main loop that processes all the BFT events.
     */
    private void eventLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Event ev = events.take();
                if (status != Status.RUNNING) {
                    continue;
                }

                switch (ev.getType()) {
                case STOP:
                    logger.debug("Received STOP event");
                    return;
                case TIMEOUT:
                    onTimeout();
                    break;
                case NEW_HEIGHT:
                    onNewHeight(ev.getData());
                    break;
                case PROPOSAL:
                    onProposal(ev.getData());
                    break;
                case VOTE:
                    onVote(ev.getData());
                    break;
                default:
                    break;
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.warn("Exception when processing consensus events", e);
            }
        }
    }

    @Override
    public void start() {
        if (status == Status.STOPPED) {
            status = Status.RUNNING;
            timer.start();
            broadcaster.start();
            logger.info("Consensus started");

            enterNewHeight(false);
            eventLoop();

            logger.info("Consensus stopped");
        }
    }

    @Override
    public void stop() {
        if (status != Status.STOPPED) {
            // interrupt sync
            if (status == Status.SYNCING) {
                sync.stop();
            }

            status = Status.STOPPED;
            timer.stop();
            broadcaster.stop();

            // kill the event handler
            events.add(new Event(Type.STOP, null));
        }
    }

    @Override
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     * Enter the NEW_HEIGHT state
     */
    protected void enterNewHeight(boolean fromSync) {
        state = State.NEW_HEIGHT;

        // update previous block
        prevBlock = chain.getLatestBlock();

        // update view state
        height = prevBlock.getNumber() + 1;
        view = 0;

        // update validators
        List<Delegate> list = delegateState.getValidators();
        List<String> peerIds = new ArrayList<>();
        list.forEach(d -> {
            peerIds.add(Hex.encode(d.getAddress()));
        });
        validators = peerIds;
        activeValidators = channelMgr.getActiveChannels(validators);
        committed = false;

        // reset proposal and votes
        proposal = null;
        resetVotes();

        logger.debug("Entered new_height: height = {}, validators = {}", height, validators.size());
        if (isValidator()) {
            timer.addTimeout(fromSync ? 0 : Config.BFT_NEW_HEIGHT_TIMEOUT);
        }

        // Broadcast NEW_HEIGHT messages to all peers, whether or not validator.
        for (Channel c : channelMgr.getActiveChannels()) {
            BFTNewHeightMessage msg = new BFTNewHeightMessage(height);
            c.getMessageQueue().sendMessage(msg);
        }
    }

    /**
     * Enter the PROPOSE state
     */
    protected void enterPropose(boolean newView) {
        state = State.PROPOSE;
        timer.addTimeout(Config.BFT_PROPOSE_TIMEOUT);

        // update active peers
        activeValidators = channelMgr.getActiveChannels(validators);

        // proof of lock
        Proof proof = Proof.NO_PROOF;
        if (newView) {
            assert (precommitVotes.isRejected());

            view++;
            proposal = null;

            proof = new Proof(precommitVotes.getRejections());
            resetVotes();
        }

        logger.debug("Entered propose: height = {}, view = {}, primary = {}, # active peers = {}", height, view,
                isPrimary(), activeValidators.size());

        if (isPrimary()) {
            if (proposal == null) {
                Block block = proposeBlock();
                proposal = new Proposal(height, view, block, proof);
                proposal.sign(coinbase);
            }

            logger.debug("Proposing: {}", proposal);
            broadcaster.broadcast(new BFTProposalMessage(proposal));
        }
    }

    /**
     * Enter the VALIDATE state
     */
    protected void enterValidate() {
        state = State.VALIDATE;
        timer.addTimeout(Config.BFT_VALIDATE_TIMEOUT);
        logger.debug("Entered validate: proposal ready = {}", proposal != null);

        Vote vote = null;
        if (proposal != null && validateBlock(proposal.getBlock())) {
            vote = Vote.newApprove(VoteType.VALIDATE, height, view, proposal.getBlock().getHash());
        } else {
            vote = Vote.newReject(VoteType.VALIDATE, height, view);
        }
        vote.sign(coinbase);

        // Always broadcast instead of relying on event handler to broadcast.
        proposalVotes.addVote(vote);
        broadcaster.broadcast(new BFTVoteMessage(vote));
    }

    /**
     * Enter the PRE_COMMIT state
     */
    protected void enterPreCommit() {
        state = State.PRE_COMMIT;
        timer.addTimeout(Config.BFT_PRE_COMMIT_TIMEOUT);
        logger.debug("Entered pre_commit: proposal votes = {}, pre_commit votes = {}", proposalVotes, precommitVotes);

        Vote vote = null;
        if (proposal != null && proposalVotes.isApproved()) {
            vote = Vote.newApprove(VoteType.PRECOMMIT, height, view, proposal.getBlock().getHash());
        } else {
            vote = Vote.newReject(VoteType.PRECOMMIT, height, view);
        }
        vote.sign(coinbase);

        precommitVotes.addVote(vote);
        broadcaster.broadcast(new BFTVoteMessage(vote));
    }

    /**
     * Enter the COMMIT state
     */
    protected void enterCommit() {
        state = State.COMMIT;
        logger.debug("Enter commit: pre-commit votes = {}, commit votes = {}", precommitVotes, commitVotes);

        if (proposal != null) {
            // [1] create a block
            Block block = proposal.getBlock();

            // [2] update view and votes
            List<Signature> votes = new ArrayList<>();
            for (Vote vote : precommitVotes.getApprovals()) {
                votes.add(vote.getSignature());
            }
            block.setView(view);
            block.setVotes(votes);

            // [3] add the block to chain
            logger.info(block.toString());
            commitBlock(block);

            // [4] broadcast COMMIT vote
            Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, block.getHash());
            vote.sign(coinbase);

            commitVotes.addVote(vote);
            broadcaster.broadcast(new BFTVoteMessage(vote));

            committed = true;
            if (commitVotes.isApproved()) {
                enterNewHeight(false);
            }
        } else {
            logger.info("Proposal not received");
        }
    }

    protected void onNewHeight(long h) {
        logger.trace("On new_height: {}", h);

        long minDiff = isValidator() ? 2 : 1;

        if (h - height >= minDiff) {
            if (activeValidators != null) {
                int count = 0;
                for (Channel c : activeValidators) {
                    if (c.isActive()) {
                        count += (c.getRemotePeer().getLatestBlockNumber() + 1 - height >= minDiff) ? 1 : 0;
                    }
                }

                if (count >= (int) Math.ceil(activeValidators.size() * 2.0 / 3.0)) {
                    resetEvents();
                    sync(h);
                    enterNewHeight(true);
                }
            }
        }
    }

    protected void onProposal(Proposal p) {
        logger.trace("On proposal: {}", p);

        if (p.getHeight() == height //
                && (p.getView() > view || p.getView() == view && proposal == null) //
                && isFromValidator(p.getSignature()) //
                && isPrimary(p.getView(), p.getSignature().getPublicKey())) {
            // check proof
            if (p.getView() != 0) {
                VoteSet vs = new VoteSet(p.getHeight(), p.getView() - 1, validators);
                vs.addVotes(p.getProof().getVotes());
                if (!vs.isRejected()) {
                    return;
                }
            } else if (!p.getProof().getVotes().isEmpty()) {
                return;
            }
            logger.trace("Proposal accepted: height = {}, view = {}", p.getHeight(), p.getView());

            // forward proposal
            BFTProposalMessage msg = new BFTProposalMessage(p);
            broadcaster.broadcast(msg);

            if (view == p.getView() && state == State.PROPOSE) {
                proposal = p;
            } else {
                height = p.getHeight();
                view = p.getView();
                proposal = p;
                resetVotes();
                resetEvents();

                // jump into PROPOSE state
                enterPropose(false);
            }
        }
    }

    protected void onVote(Vote v) {
        logger.trace("On vote: {}", v);

        if (v.getHeight() == height //
                && v.getView() == view //
                && isFromValidator(v.getSignature()) //
                && v.validate()) {
            boolean added = false;

            switch (v.getType()) {
            case VALIDATE:
                added = proposalVotes.addVote(v);
                break;
            case PRECOMMIT:
                added = precommitVotes.addVote(v);
                break;
            case COMMIT:
                added = commitVotes.addVote(v);

                if (commitVotes.isApproved()) {
                    if (committed) {
                        enterNewHeight(false);
                    } else if (precommitVotes.isApproved()) {
                        enterCommit();
                    }
                }
                break;
            }

            if (added) {
                BFTVoteMessage msg = new BFTVoteMessage(v);
                broadcaster.broadcast(msg);
            }
        }
    }

    @Override
    public boolean onMessage(Channel channel, Message msg) {
        if (!isRunning()) {
            return false;
        }

        switch (msg.getCode()) {
        case BFT_NEW_HEIGHT: {
            BFTNewHeightMessage m = (BFTNewHeightMessage) msg;

            // update peer height state
            channel.getRemotePeer().setLatestBlockNumber(m.getHeight() - 1);

            events.add(new Event(Event.Type.NEW_HEIGHT, m.getHeight()));
            return true;
        }
        case BFT_PROPOSAL: {
            BFTProposalMessage m = (BFTProposalMessage) msg;
            Proposal proposal = m.getProposal();

            if (proposal.getHeight() == height) {
                if (proposal.validate()) {
                    events.add(new Event(Event.Type.PROPOSAL, m.getProposal()));
                } else {
                    logger.debug("Invalid proposal from {}", channel.getRemotePeer().getPeerId());
                    channel.getMessageQueue().disconnect(ReasonCode.CONSENSUS_ERROR);
                }
            }
            return true;
        }
        case BFT_VOTE: {
            BFTVoteMessage m = (BFTVoteMessage) msg;
            Vote vote = m.getVote();

            if (vote.getHeight() == height) {
                if (vote.validate()) {
                    events.add(new Event(Event.Type.VOTE, vote));
                } else {
                    logger.debug("Invalid vote from {}", channel.getRemotePeer().getPeerId());
                    channel.getMessageQueue().disconnect(ReasonCode.CONSENSUS_ERROR);
                }
            }
            return true;
        }
        default:
            return false;
        }
    }

    /**
     * Timeout handler
     */
    protected void onTimeout() {
        switch (state) {
        case NEW_HEIGHT:
            enterPropose(false);
            break;
        case PROPOSE:
            enterValidate();
            break;
        case VALIDATE:
            enterPreCommit();
            break;
        case PRE_COMMIT:
            if (precommitVotes.isApproved()) {
                enterCommit();
            } else if (precommitVotes.isRejected()) {
                enterPropose(true);
            } else {
                enterPropose(false);
            }
            break;
        case COMMIT:
            // do nothing
            break;
        }
    }

    /**
     * Check if this node is a validator.
     * 
     * @return
     */
    protected boolean isValidator() {
        return validators.contains(coinbase.toAddressString());
    }

    /**
     * Check if this node is the primary validator for this view.
     * 
     * @return
     */
    protected boolean isPrimary() {
        return isPrimary(view, coinbase.getPublicKey());
    }

    /**
     * Check if a node is the primary for the specified view.
     * 
     * @param view
     *            a specific view
     * @param pubKey
     *            public key
     * @return
     */
    protected boolean isPrimary(int view, byte[] pubKey) {
        String peerId = Hex.encode(Hash.h160(pubKey));

        int p = Bytes.toInt(prevBlock.getHash()) + view;
        p = p % validators.size();
        p = (p >= 0) ? p : p + validators.size();

        return validators.get(p).equals(peerId);
    }

    /**
     * Check if the signature is from one of the validators.
     * 
     * @param sig
     * @return
     */
    protected boolean isFromValidator(Signature sig) {
        return validators.contains(Hex.encode(Hash.h160(sig.getPublicKey())));
    }

    /**
     * Reset all vote sets. This should be invoked whenever height or view changes.
     */
    protected void resetVotes() {
        proposalVotes = new VoteSet(height, view, validators);
        precommitVotes = new VoteSet(height, view, validators);
        commitVotes = new VoteSet(height, view, validators);
    }

    /**
     * Clear all timer and internal events.
     */
    protected void resetEvents() {
        timer.clear();
        events.clear();
    }

    /**
     * Create a block for BFT proposal.
     * 
     * @return the proposed block
     */
    protected Block proposeBlock() {
        long t1 = System.currentTimeMillis();
        List<Transaction> txs = new ArrayList<>();

        AccountState as = accountState.track();
        DelegateState ds = delegateState.track();
        TransactionExecutor exec = TransactionExecutor.getInstance();

        // validate transactions
        List<Transaction> list = pendingMgr.getTransactions(Config.MAX_BLOCK_SIZE);
        List<TransactionResult> results = exec.execute(list, as, ds, false);
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).isValid()) {
                txs.add(list.get(i));
            }
        }

        // build merkle tree
        List<byte[]> hashes = new ArrayList<>();
        for (Transaction tx : txs) {
            hashes.add(tx.getHash());
        }
        MerkleTree merkle = new MerkleTree(hashes);

        // construct block
        long number = height;
        byte[] prevHash = chain.getBlockHash(height - 1);
        long timestamp = System.currentTimeMillis();
        byte[] merkleRoot = merkle.getRootHash();
        byte[] data = {};
        Block block = new Block(number, coinbase.toAddress(), prevHash, timestamp, merkleRoot, data, txs);
        block.sign(coinbase);

        long t2 = System.currentTimeMillis();
        logger.debug("Block creation: # txs = {}, time = {} ms", txs.size(), t2 - t1);

        return block;
    }

    /**
     * Check if a block is valid.
     * 
     * NOTOE: this method will NOT check the block data integrity and signature
     * validity. Use {@link Block#validate()} at that purpose.
     * 
     * @param block
     * @return
     */
    protected boolean validateBlock(Block block) {
        // [1] check block integrity and signature
        if (!block.validate()) {
            return false;
        }

        // [2] check number and prevHash
        Block latest = chain.getLatestBlock();
        if (block.getNumber() != latest.getNumber() + 1 || !Arrays.equals(block.getPrevHash(), latest.getHash())) {
            return false;
        }

        AccountState as = accountState.track();
        DelegateState ds = delegateState.track();
        TransactionExecutor exec = TransactionExecutor.getInstance();

        // [3] check transactions
        List<TransactionResult> results = exec.execute(block.getTransactions(), as, ds, false);
        for (int i = 0; i < results.size(); i++) {
            if (!results.get(i).isValid()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Commit a block to the blockchain.
     * 
     * @param block
     */
    protected void commitBlock(Block block) {
        AccountState as = chain.getAccountState().track();
        DelegateState ds = chain.getDeleteState().track();

        // [1] execute all transactions
        TransactionExecutor exec = TransactionExecutor.getInstance();
        List<TransactionResult> results = exec.execute(block.getTransactions(), as, ds, true);
        for (int i = 0; i < results.size(); i++) {
            if (!results.get(i).isValid()) {
                byte[] hash = block.getTransactions().get(i).getHash();
                logger.warn("Invalid transaction bypassed the consensus, tx = {}", Hex.encode(hash));
                return;
            }
        }

        // [2] apply block reward and tx fees
        long reward = Config.getBlockReward(block.getNumber());
        for (Transaction tx : block.getTransactions()) {
            reward += tx.getFee();
        }
        if (reward > 0) {
            Account acc = chain.getAccountState().getAccount(block.getCoinbase());
            acc.setBalance(acc.getBalance() + reward);
        }

        // [3] add block to chain
        chain.addBlock(block);

        // [4] flush state updates to disk
        chain.getAccountState().commit();
        chain.getDeleteState().commit();
    }

    public enum State {
        NEW_HEIGHT, PROPOSE, VALIDATE, PRE_COMMIT, COMMIT
    }

    public class Timer {
        private PriorityBlockingQueue<Long> pq = new PriorityBlockingQueue<>();

        private Thread t;

        public void start() {
            if (t == null) {
                t = new Thread(() -> {
                    while (true) {
                        try {
                            Long t = pq.take();

                            if (System.currentTimeMillis() > t) {
                                events.add(new Event(Type.TIMEOUT));
                            } else {
                                pq.add(t);
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }, "cons-timer");
                t.start();
            }
        }

        public void stop() {
            if (t != null) {
                try {
                    t.interrupt();
                    t.join(10000);
                } catch (InterruptedException e) {
                    logger.warn("Got interrupted when stopping consenus timer");
                }

                if (t.isAlive()) {
                    logger.error("Failed to stop consensus timer");
                }
                t = null;
            }
        }

        public void addTimeout(long miliseconds) {
            if (miliseconds < 0) {
                throw new IllegalArgumentException("Timeout can not be negative");
            }
            pq.add(System.currentTimeMillis() + miliseconds);
        }

        public void clear() {
            pq.clear();
        }
    }

    public class Broadcaster {
        private BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

        private Thread t;

        public void start() {
            if (t == null) {
                t = new Thread(() -> {
                    while (true) {
                        try {
                            Message msg = queue.take();

                            // thread-safe via volatile
                            List<Channel> channels = activeValidators;
                            if (channels != null) {
                                int[] indexes = ArrayUtil
                                        .permutation(Math.min(Config.NET_RELAY_REDUNDANCY, channels.size()));
                                for (int idx : indexes) {
                                    if (channels.get(idx).isActive()) {
                                        channels.get(idx).getMessageQueue().sendMessage(msg);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }, "cons-broadcaster");
                t.start();
            }
        }

        public void stop() {
            if (t != null) {
                try {
                    t.interrupt();
                    t.join();
                } catch (InterruptedException e) {
                    logger.error("Failed to stop consensus broadcaster, interrupted");
                }
                t = null;
            }
        }

        public void broadcast(Message msg) {
            queue.offer(msg);
        }
    }

    public static class Event {
        public enum Type {
            /**
             * Received a timeout signal.
             */
            TIMEOUT,

            /**
             * Received a stop signal.
             */
            STOP,

            /**
             * Received a new height message.
             */
            NEW_HEIGHT,

            /**
             * Received a proposal message.
             */
            PROPOSAL,

            /**
             * Received a vote message.
             */
            VOTE
        }

        private Type type;
        private Object data;

        public Event(Type type) {
            this(type, null);
        }

        public Event(Type type, Object data) {
            this.type = type;
            this.data = data;
        }

        public Type getType() {
            return type;
        }

        @SuppressWarnings("unchecked")
        public <T> T getData() {
            return (T) data;
        }

        @Override
        public String toString() {
            return "Event [type=" + type + ", data=" + data + "]";
        }
    }

    public enum Status {
        STOPPED, RUNNING, SYNCING
    }
}
