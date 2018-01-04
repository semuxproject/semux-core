/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.consensus.SemuxBFT.Event.Type;
import org.semux.consensus.exception.SemuxBFTException;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.Consensus;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
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
import org.semux.net.msg.MessageCode;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.BFTNewHeightMessage;
import org.semux.net.msg.consensus.BFTNewViewMessage;
import org.semux.net.msg.consensus.BFTProposalMessage;
import org.semux.net.msg.consensus.BFTVoteMessage;
import org.semux.util.ArrayUtil;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class SemuxBFT implements Consensus {
    static final Logger logger = LoggerFactory.getLogger(SemuxBFT.class);

    protected Kernel kernel;
    protected Config config;

    protected Blockchain chain;
    protected ChannelManager channelMgr;
    protected PendingManager pendingMgr;
    protected SyncManager sync;

    protected EdDSA coinbase;

    protected AccountState accountState;
    protected DelegateState delegateState;

    protected Timer timer;
    protected Broadcaster broadcaster;
    protected BlockingQueue<Event> events = new LinkedBlockingQueue<>();

    protected volatile Status status;
    protected volatile State state;

    protected long height;
    protected int view;
    protected Proof proof;
    protected Proposal proposal;

    protected Cache<ByteArray, Block> validBlocks = Caffeine.newBuilder().maximumSize(8).build();

    protected volatile List<String> validators;
    protected volatile List<Channel> activeValidators;
    protected volatile long lastUpdate;

    protected VoteSet validateVotes;
    protected VoteSet precommitVotes;
    protected VoteSet commitVotes;

    public SemuxBFT(Kernel kernel) {
        this.kernel = kernel;
        this.config = kernel.getConfig();

        this.chain = kernel.getBlockchain();
        this.channelMgr = kernel.getChannelManager();
        this.pendingMgr = kernel.getPendingManager();
        this.sync = kernel.getSyncManager();
        this.coinbase = kernel.getCoinbase();

        this.accountState = chain.getAccountState();
        this.delegateState = chain.getDelegateState();

        this.timer = new Timer();
        this.broadcaster = new Broadcaster();

        this.status = Status.STOPPED;
        this.state = State.NEW_HEIGHT;
    }

    /**
     * Pause the consensus, and do synchronization.
     * 
     */
    protected void sync(long target) {
        if (status == Status.RUNNING) {
            // change status
            status = Status.SYNCING;

            // reset votes, timer, and events
            clearVotes();
            clearTimerAndEvents();

            // start syncing
            sync.start(target);

            // restore status if not stopped
            if (status != Status.STOPPED) {
                status = Status.RUNNING;

                // enter new height
                enterNewHeight();
            }
        }
    }

    /**
     * Main loop that processes all the BFT events.
     */
    protected void eventLoop() {
        while (!Thread.currentThread().isInterrupted() && status != Status.STOPPED) {
            try {
                Event ev = events.take();
                if (status != Status.RUNNING) {
                    continue;
                }

                // in case we get stuck at one height for too long
                if (lastUpdate + 2 * 60 * 1000L < System.currentTimeMillis()) {
                    updateValidators();
                }

                switch (ev.getType()) {
                case STOP:
                    return;
                case TIMEOUT:
                    onTimeout();
                    break;
                case NEW_HEIGHT:
                    onNewHeight(ev.getData());
                    break;
                case NEW_VIEW:
                    onNewView(ev.getData());
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
                logger.info("Consensus got interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Unexpected exception in event loop", e);
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

            enterNewHeight();
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

            timer.stop();
            broadcaster.stop();

            status = Status.STOPPED;
            Event ev = new Event(Event.Type.STOP);
            if (!events.offer(ev)) {
                logger.error("Failed to add an event to message queue: ev = {}", ev);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     * Enter the NEW_HEIGHT state
     */
    protected void enterNewHeight() {
        state = State.NEW_HEIGHT;

        // update previous block
        Block prevBlock = chain.getLatestBlock();

        // update view state
        height = prevBlock.getNumber() + 1;
        view = 0;
        proof = new Proof(height, view);
        proposal = null;

        // update validators
        updateValidators();

        // reset votes and events
        clearVotes();
        clearTimerAndEvents();

        logger.info("Entered new_height: height = {}, # validators = {}", height, validators.size());
        if (isValidator()) {
            if (this.config.networkId() == Constants.MAIN_NET_ID && !SystemUtil.bench()) {
                logger.error("You need to upgrade your computer to join the BFT consensus!");
                SystemUtil.exitAsync(-1);
            }
            resetTimeout(config.bftNewHeightTimeout());
        }

        // Broadcast NEW_HEIGHT messages to ALL peers.
        BFTNewHeightMessage msg = new BFTNewHeightMessage(height);
        for (Channel c : channelMgr.getActiveChannels()) {
            c.getMessageQueue().sendMessage(msg);
        }
    }

    /**
     * Enter the PROPOSE state
     */
    protected void enterPropose() {
        state = State.PROPOSE;
        resetTimeout(config.bftProposeTimeout());

        updateValidators();

        if (precommitVotes.isRejected()) {
            view++;
            proof = new Proof(height, view, precommitVotes.getRejections());

            proposal = null;
            clearVotes();
        }

        logger.info("Entered propose: height = {}, view = {}, primary = {}, # connected validators = 1 + {}", height,
                view, isPrimary(), activeValidators.size());

        if (isPrimary()) {
            if (proposal == null) {
                Block block = proposeBlock();
                proposal = new Proposal(proof, block.getHeader(), block.getTransactions());
                proposal.sign(coinbase);
            }

            logger.debug("Proposing: {}", proposal);
            broadcaster.broadcast(new BFTProposalMessage(proposal));
        }

        // broadcast NEW_VIEW messages.
        BFTNewViewMessage msg = new BFTNewViewMessage(proof);
        for (Channel c : activeValidators) {
            c.getMessageQueue().sendMessage(msg);
        }
    }

    /**
     * Enter the VALIDATE state
     */
    protected void enterValidate() {
        state = State.VALIDATE;
        resetTimeout(config.bftValidateTimeout());
        logger.info("Entered validate: proposal = {}, votes = {} {} {}", proposal != null, validateVotes,
                precommitVotes, commitVotes);

        // validate block proposal
        boolean valid = (proposal != null) && validateBlock(proposal.getBlockHeader(), proposal.getTransactions());

        // construct vote
        Vote vote = valid ? Vote.newApprove(VoteType.VALIDATE, height, view, proposal.getBlockHeader().getHash())
                : Vote.newReject(VoteType.VALIDATE, height, view);
        vote.sign(coinbase);

        // always broadcast vote directly.
        validateVotes.addVote(vote);
        broadcaster.broadcast(new BFTVoteMessage(vote));
    }

    /**
     * Enter the PRE_COMMIT state
     */
    protected void enterPreCommit() {
        state = State.PRE_COMMIT;
        resetTimeout(config.bftPreCommitTimeout());
        logger.info("Entered pre_commit: proposal = {}, votes = {} {} {}", proposal != null, validateVotes,
                precommitVotes, commitVotes);

        // vote YES as long as +2/3 validators received a success block proposal
        Optional<byte[]> blockHash = validateVotes.anyApproved();
        Vote vote = blockHash.map(bytes -> Vote.newApprove(VoteType.PRECOMMIT, height, view, bytes))
                .orElseGet(() -> Vote.newReject(VoteType.PRECOMMIT, height, view));
        vote.sign(coinbase);

        // always broadcast vote directly.
        precommitVotes.addVote(vote);
        broadcaster.broadcast(new BFTVoteMessage(vote));
    }

    /**
     * Enter the COMMIT state
     */
    protected void enterCommit() {
        state = State.COMMIT;
        resetTimeout(config.bftCommitTimeout());
        logger.info("Entered commit: proposal = {}, votes = {} {} {}", proposal != null, validateVotes, precommitVotes,
                commitVotes);

        Optional<byte[]> blockHash = precommitVotes.anyApproved();
        if (!blockHash.isPresent()) {
            throw new SemuxBFTException("Entered COMMIT state without +2/3 pre-commit votes");
        } else {
            // create a COMMIT vote
            Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, blockHash.get());
            vote.sign(coinbase);

            // always broadcast vote directly.
            commitVotes.addVote(vote);
            broadcaster.broadcast(new BFTVoteMessage(vote));
        }
    }

    /**
     * Enter the FINALIZE state
     */
    protected void enterFinalize() {
        // make sure we only enter FINALIZE state once per height
        if (state == State.FINALIZE) {
            return;
        }

        state = State.FINALIZE;
        resetTimeout(config.bftFinalizeTimeout());
        logger.info("Entered finalize: proposal = {}, votes = {} {} {}", proposal != null, validateVotes,
                precommitVotes, commitVotes);

        Optional<byte[]> blockHash = precommitVotes.anyApproved();
        Block block;
        if (blockHash.isPresent() && (block = validBlocks.getIfPresent(ByteArray.of(blockHash.get()))) != null) {
            // [1] update view and votes
            List<Signature> votes = new ArrayList<>();
            for (Vote vote : precommitVotes.getApprovals(blockHash.get())) {
                votes.add(vote.getSignature());
            }
            block.setView(view);
            block.setVotes(votes);

            // [2] add the block to chain
            logger.info(block.toString());
            applyBlock(block);
        } else {
            sync(height + 1);
        }
    }

    protected void resetTimeout(long timeout) {
        timer.timeout(timeout);

        events.removeIf(e -> e.type == Type.TIMEOUT);
    }

    protected void jumpToView(int view, Proof proof, Proposal proposal) {
        this.view = view;
        this.proof = proof;
        this.proposal = proposal;
        clearVotes();
        clearTimerAndEvents();

        // enter PROPOSE state
        enterPropose();
    }

    /**
     * Synchronization will be started if the 2/3th active validator's height
     * (sorted by latest block number) is greater than local height. This avoids a
     * vulnerability that malicious validators might announce an extremely large
     * height in order to hang sync process of peers.
     * 
     * @param newHeight
     *            new height
     */
    protected void onNewHeight(long newHeight) {
        // Get the latest status of local blockchain and active validators.
        // We don't call updateValidators() here in order to avoid affecting block
        // proposal.
        Long currentHeight = chain.getLatestBlockNumber();
        List<String> currentValidators = chain.getValidators();
        List<Channel> currentActiveValidators = channelMgr.getActiveChannels(currentValidators);

        logger.trace(
                "On new_height: {}, current height = {}, validators = {}, active validators = {}",
                newHeight,
                currentHeight,
                currentValidators.size(),
                currentActiveValidators.size());

        if (newHeight > currentHeight && !currentActiveValidators.isEmpty()) {
            // Pick 2/3th active validator's height as sync target. The sync will not be
            // started if there are less than 2 active validators.
            OptionalLong targetOptional = currentActiveValidators.stream()
                    .mapToLong(c -> c.getRemotePeer().getLatestBlockNumber() + 1)
                    .sorted()
                    .limit((int) Math.floor(currentActiveValidators.size() * 2.0 / 3.0))
                    .max();

            if (targetOptional.isPresent() && targetOptional.getAsLong() > currentHeight) {
                sync(targetOptional.getAsLong());
            }
        }
    }

    protected void onNewView(Proof p) {
        logger.trace("On new_view: {}", p);

        if (p.getHeight() == height // at same height
                && p.getView() > view && state != State.COMMIT && state != State.FINALIZE) {// larger view

            // check proof-of-unlock
            VoteSet vs = new VoteSet(VoteType.PRECOMMIT, p.getHeight(), p.getView() - 1, validators);
            vs.addVotes(p.getVotes());
            if (!vs.isRejected()) {
                return;
            }

            // switch view
            logger.debug("Switching view because of NEW_VIEW message");
            jumpToView(p.getView(), p, null);
        }
    }

    protected void onProposal(Proposal p) {
        logger.trace("On proposal: {}", p);

        if (p.getHeight() == height // at the same height
                && (p.getView() == view && proposal == null && (state == State.NEW_HEIGHT || state == State.PROPOSE) // expecting
                        || p.getView() > view && state != State.COMMIT && state != State.FINALIZE) // larger view
                && isPrimary(p.getHeight(), p.getView(), Hex.encode(p.getSignature().getAddress()))) {

            // check proof-of-unlock
            if (p.getView() != 0) {
                VoteSet vs = new VoteSet(VoteType.PRECOMMIT, p.getHeight(), p.getView() - 1, validators);
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

            if (view == p.getView()) {
                proposal = p;
            } else {
                // switch view
                logger.debug("Switching view because of PROPOSE message");
                jumpToView(p.getView(), p.getProof(), p);
            }
        }
    }

    protected void onVote(Vote v) {
        logger.trace("On vote: {}", v);

        if (v.getHeight() == height
                && v.getView() == view
                && isFromValidator(v.getSignature())
                && v.validate()) {
            boolean added = false;

            switch (v.getType()) {
            case VALIDATE:
                added = validateVotes.addVote(v);
                break;
            case PRECOMMIT:
                added = precommitVotes.addVote(v);
                break;
            case COMMIT:
                added = commitVotes.addVote(v);
                if (commitVotes.anyApproved().isPresent()) {
                    // skip COMMIT state time out if +2/3 commit votes
                    enterFinalize();
                }
                break;
            }

            if (added) {
                BFTVoteMessage msg = new BFTVoteMessage(v);
                broadcaster.broadcast(msg);
            }
        }
    }

    /**
     * Timeout handler
     */
    protected void onTimeout() {
        switch (state) {
        case NEW_HEIGHT:
            enterPropose();
            break;
        case PROPOSE:
            enterValidate();
            break;
        case VALIDATE:
            enterPreCommit();
            break;
        case PRE_COMMIT:
            if (precommitVotes.anyApproved().isPresent()) {
                enterCommit();
            } else {
                enterPropose();
            }
            break;
        case COMMIT:
            enterFinalize();
            break;
        case FINALIZE:
            enterNewHeight();
            break;
        }
    }

    @Override
    public void onMessage(Channel channel, Message msg) {
        // only process BFT_NEW_HEIGHT message when not running
        if (!isRunning() && msg.getCode() != MessageCode.BFT_NEW_HEIGHT) {
            return;
        }

        switch (msg.getCode()) {
        case BFT_NEW_HEIGHT: {
            BFTNewHeightMessage m = (BFTNewHeightMessage) msg;

            // update peer height state
            channel.getRemotePeer().setLatestBlockNumber(m.getHeight() - 1);

            if (m.getHeight() > height) {
                events.add(new Event(Event.Type.NEW_HEIGHT, m.getHeight()));
            }
            break;
        }
        case BFT_NEW_VIEW: {
            BFTNewViewMessage m = (BFTNewViewMessage) msg;

            // update peer height state
            channel.getRemotePeer().setLatestBlockNumber(m.getHeight() - 1);

            if (m.getHeight() > height) {
                events.add(new Event(Event.Type.NEW_HEIGHT, m.getHeight()));
            } else if (m.getHeight() == height) {
                events.add(new Event(Event.Type.NEW_VIEW, m.getProof()));
            }
            break;
        }
        case BFT_PROPOSAL: {
            BFTProposalMessage m = (BFTProposalMessage) msg;
            Proposal p = m.getProposal();

            if (p.getHeight() == height) {
                if (p.validate()) {
                    events.add(new Event(Event.Type.PROPOSAL, m.getProposal()));
                } else {
                    logger.debug("Invalid proposal from {}", channel.getRemotePeer().getPeerId());
                    channel.getMessageQueue().disconnect(ReasonCode.BAD_PEER);
                }
            }
            break;
        }
        case BFT_VOTE: {
            BFTVoteMessage m = (BFTVoteMessage) msg;
            Vote vote = m.getVote();

            if (vote.getHeight() == height) {
                if (vote.validate()) {
                    events.add(new Event(Event.Type.VOTE, vote));
                } else {
                    logger.debug("Invalid vote from {}", channel.getRemotePeer().getPeerId());
                    channel.getMessageQueue().disconnect(ReasonCode.BAD_PEER);
                }
            }
            break;
        }
        default: {
            break;
        }
        }
    }

    /**
     * Update the validator sets.
     */
    protected void updateValidators() {
        validators = chain.getValidators();
        activeValidators = channelMgr.getActiveChannels(validators);
        lastUpdate = System.currentTimeMillis();
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
        return isPrimary(height, view, coinbase.toAddressString());
    }

    /**
     * Check if a node is the primary for the specified view.
     * 
     * 
     * @param height
     *            block number
     * @param view
     *            a specific view
     * @param peerId
     *            peer id
     * @return
     */
    protected boolean isPrimary(long height, int view, String peerId) {
        return config.getPrimaryValidator(validators, height, view).equals(peerId);
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
    protected void clearVotes() {
        validateVotes = new VoteSet(VoteType.VALIDATE, height, view, validators);
        precommitVotes = new VoteSet(VoteType.PRECOMMIT, height, view, validators);
        commitVotes = new VoteSet(VoteType.COMMIT, height, view, validators);
    }

    /**
     * Reset timer and events.
     */
    protected void clearTimerAndEvents() {
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

        // fetch pending transactions
        final List<PendingManager.PendingTransaction> pending = pendingMgr
                .getPendingTransactions(config.maxBlockTransactionsSize());
        final List<Transaction> pendingTxs = pending.stream()
                .map(tx -> tx.transaction)
                .collect(Collectors.toList());
        final List<TransactionResult> pendingResults = pending.stream()
                .map(tx -> tx.transactionResult)
                .collect(Collectors.toList());

        // compute roots
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(pendingTxs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(pendingResults);
        byte[] stateRoot = Bytes.EMPTY_HASH;

        // construct block
        long number = height;
        byte[] prevHash = chain.getBlockHeader(height - 1).getHash();
        long timestamp = System.currentTimeMillis();
        byte[] data = {};
        BlockHeader header = new BlockHeader(number, coinbase.toAddress(), prevHash, timestamp, transactionsRoot,
                resultsRoot, stateRoot, data);
        Block block = new Block(header, pendingTxs, pendingResults);

        long t2 = System.currentTimeMillis();
        logger.debug("Block creation: # txs = {}, time = {} ms", pendingTxs.size(), t2 - t1);

        return block;
    }

    /**
     * Check if a block proposal is success.
     * 
     * @param header
     * @param transactions
     * @return
     */
    protected boolean validateBlock(BlockHeader header, List<Transaction> transactions) {
        long t1 = System.currentTimeMillis();

        // [1] check block header
        Block latest = chain.getLatestBlock();
        if (!Block.validateHeader(latest.getHeader(), header)) {
            logger.debug("Invalid block header");
            return false;
        }

        // [2] check transactions and results (skipped)
        if (!Block.validateTransactions(header, transactions, config.networkId())
                || transactions.stream().mapToInt(Transaction::size).sum() > config.maxBlockTransactionsSize()) {
            logger.debug("Invalid block transactions");
            return false;
        }
        if (transactions.stream().anyMatch(tx -> chain.hasTransaction(tx.getHash()))) {
            logger.warn("Duplicated transaction hash is not allowed");
            return false;
        }

        AccountState as = accountState.track();
        DelegateState ds = delegateState.track();
        TransactionExecutor exec = new TransactionExecutor(config);

        // [3] evaluate transactions
        List<TransactionResult> results = exec.execute(transactions, as, ds);
        if (!Block.validateResults(header, results)) {
            logger.debug("Invalid transactions");
            return false;
        }

        long t2 = System.currentTimeMillis();
        logger.debug("Block validation: # txs = {}, time = {} ms", transactions.size(), t2 - t1);

        Block block = new Block(header, transactions, results);
        validBlocks.put(ByteArray.of(block.getHash()), block);
        return true;
    }

    /**
     * Apply a block to the chain.
     * 
     * @param block
     */
    protected void applyBlock(Block block) {
        BlockHeader header = block.getHeader();
        List<Transaction> transactions = block.getTransactions();
        long number = header.getNumber();

        if (header.getNumber() > config.mandatoryUpgrade()) {
            throw new SemuxBFTException("This client needs to be upgraded");
        } else if (header.getNumber() != chain.getLatestBlockNumber() + 1) {
            throw new SemuxBFTException("Applying wrong block: number = " + header.getNumber());
        }

        // [1] check block header, skipped

        // [2] check transactions and results, skipped

        AccountState as = chain.getAccountState().track();
        DelegateState ds = chain.getDelegateState().track();
        TransactionExecutor exec = new TransactionExecutor(config);

        // [3] evaluate all transactions
        List<TransactionResult> results = exec.execute(transactions, as, ds);
        if (!Block.validateResults(header, results)) {
            logger.debug("Invalid transactions");
            return;
        }

        // [4] evaluate votes, skipped

        // [5] apply block reward and tx fees
        long reward = config.getBlockReward(number);
        for (Transaction tx : block.getTransactions()) {
            reward += tx.getFee();
        }
        if (reward > 0) {
            as.adjustAvailable(block.getCoinbase(), reward);
        }

        // [6] commit the updates
        as.commit();
        ds.commit();

        WriteLock lock = kernel.getStateLock().writeLock();
        lock.lock();
        try {
            // [7] flush state to disk
            chain.getAccountState().commit();
            chain.getDelegateState().commit();

            // [8] add block to chain
            chain.addBlock(block);
        } finally {
            lock.unlock();
        }
    }

    public enum State {
        NEW_HEIGHT, PROPOSE, VALIDATE, PRE_COMMIT, COMMIT, FINALIZE
    }

    /**
     * Timer used by consensus. It's designed to be single timeout; previous timeout
     * get cleared when new one being added.
     * 
     * NOTE: it's possible that a Timeout event has been emitted when setting a new
     * timeout.
     */
    public class Timer implements Runnable {
        private long timeout;

        private Thread t;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    if (timeout != -1 && timeout < System.currentTimeMillis()) {
                        events.add(new Event(Type.TIMEOUT));
                        timeout = -1;
                        continue;
                    }
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public synchronized void start() {
            if (t == null) {
                t = new Thread(this, "cons-timer");
                t.start();
            }
        }

        public synchronized void stop() {
            if (t != null) {
                try {
                    t.interrupt();
                    t.join(10000);
                } catch (InterruptedException e) {
                    logger.warn("Failed to stop consensus timer");
                    Thread.currentThread().interrupt();
                }
                t = null;
            }
        }

        public synchronized void timeout(long milliseconds) {
            if (milliseconds < 0) {
                throw new IllegalArgumentException("Timeout can not be negative");
            }
            timeout = System.currentTimeMillis() + milliseconds;
        }

        public synchronized void clear() {
            timeout = -1;
        }
    }

    public class Broadcaster implements Runnable {
        private BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

        private Thread t;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = queue.take();

                    // thread-safety via volatile
                    List<Channel> channels = activeValidators;
                    if (channels != null) {
                        int[] indices = ArrayUtil.permutation(channels.size());
                        for (int i = 0; i < indices.length && i < config.netRelayRedundancy(); i++) {
                            Channel c = channels.get(indices[i]);
                            if (c.isActive()) {
                                c.getMessageQueue().sendMessage(msg);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public synchronized void start() {
            if (t == null) {
                t = new Thread(this, "cons-relay");
                t.start();
            }
        }

        public synchronized void stop() {
            if (t != null) {
                try {
                    t.interrupt();
                    t.join();
                } catch (InterruptedException e) {
                    logger.error("Failed to stop consensus broadcaster");
                    Thread.currentThread().interrupt();
                }
                t = null;
            }
        }

        public void broadcast(Message msg) {
            if (!queue.offer(msg)) {
                logger.error("Failed to add a message to the broadcast queue: msg = {}", msg);
            }
        }
    }

    public static class Event {
        public enum Type {
            /**
             * Stop signal
             */
            STOP,

            /**
             * Received a timeout signal.
             */
            TIMEOUT,

            /**
             * Received a new height message.
             */
            NEW_HEIGHT,

            /**
             * Received a new view message.
             */
            NEW_VIEW,

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
