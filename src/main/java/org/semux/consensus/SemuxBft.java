/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.consensus;

import static org.semux.core.Fork.UNIFORM_DISTRIBUTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.ethereum.vm.client.BlockStore;
import org.semux.Kernel;
import org.semux.Network;
import org.semux.config.Config;
import org.semux.consensus.SemuxBft.Event.Type;
import org.semux.consensus.exception.SemuxBftException;
import org.semux.core.BftManager;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.PendingManager;
import org.semux.core.SyncManager;
import org.semux.core.Transaction;
import org.semux.core.TransactionExecutor;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.core.state.DelegateState;
import org.semux.crypto.Hash;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.crypto.Key.Signature;
import org.semux.net.Channel;
import org.semux.net.ChannelManager;
import org.semux.net.msg.Message;
import org.semux.net.msg.MessageCode;
import org.semux.net.msg.ReasonCode;
import org.semux.net.msg.consensus.NewHeightMessage;
import org.semux.net.msg.consensus.NewViewMessage;
import org.semux.net.msg.consensus.ProposalMessage;
import org.semux.net.msg.consensus.VoteMessage;
import org.semux.util.ArrayUtil;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.SystemUtil;
import org.semux.util.TimeUtil;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Implements Semux BFT engine based on single-thread event model. States are
 * maintained in the engine and are updated only by the event loop.
 * <p>
 * Asides the main event hub, there are complementary threads:
 * <code>timer</code> and <code>broadcaster</code>. The <code>timer</code>
 * thread emits a TIMEOUT event when the internal timer times out. The
 * <code>broadcaster</code> thread is responsible for relaying BFT messages to
 * peers.
 * <p>
 * The BFT engine may be one of the following status:
 * <ul>
 * <li><code>STOPPED</code>: not started</li>
 * <li><code>SYNCING</code>: waiting for syncing</li>
 * <li><code>RUNNING</code>: working</li>
 * </ul>
 * <p>
 * It is also a state machine; the possible states include:
 * <ul>
 * <li><code>NEW_HEIGHT</code>: the initial state when started</li>
 * <li><code>PROPOSE</code>: gossip block proposal</li>
 * <li><code>VALIDATE</code>: gossip VALIDATE votes between validators</li>
 * <li><code>PRE_COMMIT</code>: gossip PRE_COMMIT votes between validators</li>
 * <li><code>COMMIT</code>: after receiving 2/3+ PRE_COMMIT votes</li>
 * <li><code>FINALIZE</code>: finalize a block</li>
 * </ul>
 */
public class SemuxBft implements BftManager {
    private static final Logger logger = LoggerFactory.getLogger(SemuxBft.class);

    protected Kernel kernel;
    protected Config config;

    protected Blockchain chain;
    protected BlockStore blockStore;

    protected ChannelManager channelMgr;
    protected PendingManager pendingMgr;
    protected SyncManager syncMgr;

    protected Key coinbase;

    protected Timer timer;
    protected Broadcaster broadcaster;
    protected BlockingQueue<Event> events = new LinkedBlockingQueue<>();

    protected Status status;
    protected State state;

    protected long height;
    protected int view;
    protected Proof proof;
    protected Proposal proposal;

    protected Cache<ByteArray, Block> validBlocks = Caffeine.newBuilder().maximumSize(8).build();

    protected List<String> validators;
    protected List<Channel> activeValidators;
    protected long lastUpdate;

    protected VoteSet validateVotes;
    protected VoteSet precommitVotes;
    protected VoteSet commitVotes;

    public SemuxBft(Kernel kernel) {
        this.kernel = kernel;
        this.config = kernel.getConfig();

        this.chain = kernel.getBlockchain();
        this.blockStore = new SemuxBlockStore(chain);
        this.channelMgr = kernel.getChannelManager();
        this.pendingMgr = kernel.getPendingManager();
        this.syncMgr = kernel.getSyncManager();
        this.coinbase = kernel.getCoinbase();

        this.timer = new Timer();
        this.broadcaster = new Broadcaster();

        this.status = Status.STOPPED;
        this.state = State.NEW_HEIGHT;
    }

    /**
     * Pause the bft manager, and do synchronization.
     */
    protected void sync(long target) {
        if (status == Status.RUNNING) {
            // change status
            status = Status.SYNCING;

            // reset votes, timer, and events
            clearVotes();
            clearTimerAndEvents();

            // start syncing
            syncMgr.start(target);

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
                if (lastUpdate + 2 * 60 * 1000L < TimeUtil.currentTimeMillis()) {
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
                logger.info("BftManager got interrupted");
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
            logger.info("Semux BFT manager started");

            enterNewHeight();
            eventLoop();

            logger.info("Semux BFT manager stopped");
        }
    }

    @Override
    public void stop() {
        if (status != Status.STOPPED) {
            // interrupt sync
            if (status == Status.SYNCING) {
                syncMgr.stop();
            }

            timer.stop();
            broadcaster.stop();

            status = Status.STOPPED;
            Event ev = new Event(Type.STOP);
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
            if (this.config.network() == Network.MAINNET && !SystemUtil.bench()) {
                logger.error("You need to upgrade your computer to join the BFT consensus!");
                SystemUtil.exitAsync(SystemUtil.Code.HARDWARE_UPGRADE_NEEDED);
            }
            resetTimeout(config.bftNewHeightTimeout());
        }

        // Broadcast NEW_HEIGHT messages to ALL peers.
        NewHeightMessage msg = new NewHeightMessage(height);
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
            broadcaster.broadcast(new ProposalMessage(proposal));
        }

        // broadcast NEW_VIEW messages.
        NewViewMessage msg = new NewViewMessage(proof);
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
        boolean valid = (proposal != null)
                && validateBlockProposal(proposal.getBlockHeader(), proposal.getTransactions());

        // construct vote
        Vote vote = valid ? Vote.newApprove(VoteType.VALIDATE, height, view, proposal.getBlockHeader().getHash())
                : Vote.newReject(VoteType.VALIDATE, height, view);
        vote.sign(coinbase);

        // always broadcast vote directly.
        validateVotes.addVote(vote);
        broadcaster.broadcast(new VoteMessage(vote));
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
        broadcaster.broadcast(new VoteMessage(vote));
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
            throw new SemuxBftException("Entered COMMIT state without +2/3 pre-commit votes");
        } else {
            // create a COMMIT vote
            Vote vote = Vote.newApprove(VoteType.COMMIT, height, view, blockHash.get());
            vote.sign(coinbase);

            // always broadcast vote directly.
            commitVotes.addVote(vote);
            broadcaster.broadcast(new VoteMessage(vote));
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
            chain.importBlock(block, false);
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
        if (newHeight > height && state != State.FINALIZE) {
            // update active validators (potential overhead)
            activeValidators = channelMgr.getActiveChannels(validators);

            // the heights of active validators
            long[] heights = activeValidators.stream()
                    .mapToLong(c -> c.getRemotePeer().getLatestBlockNumber() + 1)
                    .sorted()
                    .toArray();

            // Needs at least one connected node to start syncing
            if (heights.length != 0) {
                int q = (int) Math.ceil(heights.length * 2.0 / 3.0);
                long h = heights[heights.length - q];
                if (h > height) {
                    sync(h);
                }
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
            logger.debug("Switching view because of NEW_VIEW message: {}", p.getView());
            jumpToView(p.getView(), p, null);
        }
    }

    protected void onProposal(Proposal p) {
        logger.trace("On proposal: {}", p);

        if (p.getHeight() == height // at the same height
                && (p.getView() == view && proposal == null && (state == State.NEW_HEIGHT || state == State.PROPOSE)
                        // expecting
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
            ProposalMessage msg = new ProposalMessage(p);
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
                VoteMessage msg = new VoteMessage(v);
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
            NewHeightMessage m = (NewHeightMessage) msg;

            // update peer height state
            channel.getRemotePeer().setLatestBlockNumber(m.getHeight() - 1);

            if (m.getHeight() > height) {
                events.add(new Event(Type.NEW_HEIGHT, m.getHeight()));
            }
            break;
        }
        case BFT_NEW_VIEW: {
            NewViewMessage m = (NewViewMessage) msg;

            // update peer height state
            channel.getRemotePeer().setLatestBlockNumber(m.getHeight() - 1);

            if (m.getHeight() > height) {
                events.add(new Event(Type.NEW_HEIGHT, m.getHeight()));
            } else if (m.getHeight() == height) {
                events.add(new Event(Type.NEW_VIEW, m.getProof()));
            }
            break;
        }
        case BFT_PROPOSAL: {
            ProposalMessage m = (ProposalMessage) msg;
            Proposal p = m.getProposal();

            if (p.getHeight() == height) {
                if (p.validate()) {
                    events.add(new Event(Type.PROPOSAL, m.getProposal()));
                } else {
                    logger.debug("Invalid proposal from {}", channel.getRemotePeer().getPeerId());
                    channel.getMessageQueue().disconnect(ReasonCode.BAD_PEER);
                }
            }
            break;
        }
        case BFT_VOTE: {
            VoteMessage m = (VoteMessage) msg;
            Vote vote = m.getVote();

            if (vote.getHeight() == height) {
                if (vote.revalidate()) {
                    events.add(new Event(Type.VOTE, vote));
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
        int maxValidators = config.spec().getNumberOfValidators(height);

        validators = chain.getValidators();
        // if the chain is reporting a larger number of validators
        // then a configuration change has occurred (like a stuck testnet)
        // so honor the configuration value
        if (validators.size() > maxValidators) {
            validators = validators.subList(0, maxValidators);
        }
        activeValidators = channelMgr.getActiveChannels(validators);
        lastUpdate = TimeUtil.currentTimeMillis();
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
     * @param height
     *            a specific height
     * @param view
     *            a specific view
     * @param peerId
     *            peer id
     * @return
     */
    protected boolean isPrimary(long height, int view, String peerId) {
        return config.spec()
                .getPrimaryValidator(validators, height, view, chain.isForkActivated(UNIFORM_DISTRIBUTION, height))
                .equals(peerId);
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
        AccountState asTrack = chain.getAccountState().track();
        DelegateState dsTrack = chain.getDelegateState().track();

        long t1 = TimeUtil.currentTimeMillis();

        // construct block template
        BlockHeader parent = chain.getBlockHeader(height - 1);
        long number = height;
        byte[] prevHash = parent.getHash();
        long timestamp = TimeUtil.currentTimeMillis();
        timestamp = timestamp > parent.getTimestamp() ? timestamp : parent.getTimestamp() + 1;
        byte[] data = chain.constructBlockHeaderDataField();
        BlockHeader tempHeader = new BlockHeader(height, coinbase.toAddress(), prevHash, timestamp, new byte[0],
                new byte[0], new byte[0], data);

        // fetch pending transactions
        final List<PendingManager.PendingTransaction> pendingTxs = pendingMgr
                .getPendingTransactions(config.poolBlockGasLimit());
        final List<Transaction> includedTxs = new ArrayList<>();
        final List<TransactionResult> includedResults = new ArrayList<>();

        TransactionExecutor exec = new TransactionExecutor(config, blockStore, chain.isVMEnabled(),
                chain.isVotingPrecompiledUpgraded(), chain.isEd25519ContractEnabled());
        SemuxBlock semuxBlock = new SemuxBlock(tempHeader, config.spec().maxBlockGasLimit());

        // only propose gas used up to configured block gas limit
        long remainingBlockGas = config.poolBlockGasLimit();
        long gasUsedInBlock = 0;
        for (PendingManager.PendingTransaction pendingTx : pendingTxs) {
            Transaction tx = pendingTx.transaction;

            // check if the remaining gas covers the declared gas limit
            long gas = tx.isVMTransaction() ? tx.getGas() : config.spec().nonVMTransactionGasCost();
            if (gas > remainingBlockGas) {
                break;
            }

            // re-evaluate the transaction
            TransactionResult result = exec.execute(tx, asTrack, dsTrack, semuxBlock, gasUsedInBlock);
            if (result.getCode().isAcceptable()) {
                includedTxs.add(tx);
                includedResults.add(result);

                // update counter
                long gasUsed = tx.isVMTransaction() ? result.getGasUsed() : config.spec().nonVMTransactionGasCost();
                remainingBlockGas -= gasUsed;
                gasUsedInBlock += gasUsed;
            }
        }

        // compute roots
        byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(includedTxs);
        byte[] resultsRoot = MerkleUtil.computeResultsRoot(includedResults);
        byte[] stateRoot = Bytes.EMPTY_HASH;

        BlockHeader header = new BlockHeader(number, coinbase.toAddress(), prevHash, timestamp, transactionsRoot,
                resultsRoot, stateRoot, data);
        Block block = new Block(header, includedTxs, includedResults);

        long t2 = TimeUtil.currentTimeMillis();
        logger.debug("Block creation: # txs = {}, time = {} ms", includedTxs.size(), t2 - t1);

        return block;
    }

    /**
     * Check if a block proposal is valid.
     */
    protected boolean validateBlockProposal(BlockHeader header, List<Transaction> transactions) {
        try {
            AccountState asTrack = chain.getAccountState().track();
            DelegateState dsTrack = chain.getDelegateState().track();

            Block block = new Block(header, transactions);
            long t1 = TimeUtil.currentTimeMillis();

            // [1] check block header
            Block latest = chain.getLatestBlock();
            if (!block.validateHeader(header, latest.getHeader())) {
                logger.warn("Invalid block header");
                return false;
            }

            // [?] additional checks by consensus
            // - disallow block time drifting;
            // - restrict the coinbase to be the proposer
            if (header.getTimestamp() - TimeUtil.currentTimeMillis() > config.bftMaxBlockTimeDrift()) {
                logger.warn("A block in the future is not allowed");
                return false;
            }
            if (!Arrays.equals(header.getCoinbase(), proposal.getSignature().getAddress())) {
                logger.warn("The coinbase should always equal to the proposer's address");
                return false;
            }

            // [2] check transactions
            List<Transaction> unvalidatedTransactions = getUnvalidatedTransactions(transactions);
            if (!block.validateTransactions(header, unvalidatedTransactions, transactions, config.network())) {
                logger.warn("Invalid transactions");
                return false;
            }
            if (transactions.stream().anyMatch(tx -> chain.hasTransaction(tx.getHash()))) {
                logger.warn("Duplicated transaction hash is not allowed");
                return false;
            }

            // [3] evaluate transactions
            TransactionExecutor transactionExecutor = new TransactionExecutor(config, blockStore, chain.isVMEnabled(),
                    chain.isVotingPrecompiledUpgraded(), chain.isEd25519ContractEnabled());
            List<TransactionResult> results = transactionExecutor.execute(transactions, asTrack, dsTrack,
                    new SemuxBlock(header, config.spec().maxBlockGasLimit()), 0);
            if (!block.validateResults(header, results)) {
                logger.error("Invalid transaction results");
                return false;
            }
            block.setResults(results); // overwrite the results

            long t2 = TimeUtil.currentTimeMillis();
            logger.debug("Block validation: # txs = {}, time = {} ms", transactions.size(), t2 - t1);

            validBlocks.put(ByteArray.of(block.getHash()), block);
            return true;
        } catch (Exception e) {
            logger.error("Unexpected exception during block proposal validation", e);
            return false;
        }
    }

    /**
     * Filter transactions to find ones that have not already been validated via the
     * pending manager.
     *
     * @param transactions
     * @return
     */
    protected List<Transaction> getUnvalidatedTransactions(List<Transaction> transactions) {

        Set<Transaction> pendingValidatedTransactions = pendingMgr.getPendingTransactions()
                .stream()
                .map(pendingTx -> pendingTx.transaction)
                .collect(Collectors.toSet());

        return transactions
                .stream()
                .filter(it -> !pendingValidatedTransactions.contains(it))
                .collect(Collectors.toList());
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
                    if (timeout != -1 && timeout < TimeUtil.currentTimeMillis()) {
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
                t = new Thread(this, "bft-timer");
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
            timeout = TimeUtil.currentTimeMillis() + milliseconds;
        }

        public synchronized void clear() {
            timeout = -1;
        }
    }

    public class Broadcaster implements Runnable {
        private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

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
                t = new Thread(this, "bft-relay");
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

        private final Type type;
        private final Object data;

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
