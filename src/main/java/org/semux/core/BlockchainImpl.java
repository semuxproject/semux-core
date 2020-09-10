/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.core.Fork.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.ethereum.vm.client.BlockStore;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.consensus.Vote;
import org.semux.consensus.VoteType;
import org.semux.core.Genesis.Premine;
import org.semux.core.event.BlockchainDatabaseUpgradingEvent;
import org.semux.core.exception.BlockchainException;
import org.semux.core.state.AccountState;
import org.semux.core.state.AccountStateImpl;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.core.state.DelegateStateImpl;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;
import org.semux.db.LeveldbDatabase;
import org.semux.event.PubSubFactory;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
import org.semux.util.TimeUtil;
import org.semux.vm.client.SemuxBlock;
import org.semux.vm.client.SemuxBlockStore;
import org.semux.vm.client.SemuxInternalTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blockchain implementation.
 *
 * <pre>
 * index DB structure:
 *
 * [0] => [latest_block_number]
 * [1] => [validators]
 * [2, address] => [validator_stats]
 *
 * [3, block_hash] => [block_number]
 * [4, transaction_hash] => [block_number, from, to] | [coinbase_transaction]
 * [5, address, n] => [transaction_hash]
 * [7] => [activated forks]
 *
 * [0xff] => [database version]
 * </pre>
 *
 * <pre>
 * block DB structure:
 *
 * [0, block_number] => [block_header]
 * [1, block_number] => [block_transactions]
 * [2, block_number] => [block_results]
 * [3, block_number] => [block_votes]
 * </pre>
 */
public class BlockchainImpl implements Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainImpl.class);

    protected static final int DATABASE_VERSION = 3;

    protected static final byte TYPE_LATEST_BLOCK_NUMBER = 0x00;
    protected static final byte TYPE_VALIDATORS = 0x01;
    protected static final byte TYPE_VALIDATOR_STATS_BY_ADDRESS = 0x02;
    protected static final byte TYPE_BLOCK_NUMBER_BY_HASH = 0x03;
    protected static final byte TYPE_BLOCK_COINBASE_BY_NUMBER = 0x07;
    protected static final byte TYPE_TRANSACTION_INDEX_BY_HASH = 0x04;
    protected static final byte TYPE_TRANSACTION_COUNT_BY_ADDRESS = 0x05;
    protected static final byte TYPE_TRANSACTION_HASH_BY_ADDRESS_AND_INDEX = 0x05;
    protected static final byte TYPE_ACTIVATED_FORKS = 0x06;
    protected static final byte TYPE_INTERNAL_TRANSACTION_COUNT_BY_ADDRESS = 0x07;
    protected static final byte TYPE_INTERNAL_TRANSACTION_BY_ADDRESS_AND_INDEX = 0x08;
    protected static final byte TYPE_DATABASE_VERSION = (byte) 0xff;

    protected static final byte TYPE_BLOCK_HEADER_BY_NUMBER = 0x00;
    protected static final byte TYPE_BLOCK_TRANSACTIONS_BY_NUMBER = 0x01;
    protected static final byte TYPE_BLOCK_RESULTS_BY_NUMBER = 0x02;
    protected static final byte TYPE_BLOCK_VOTES_BY_NUMBER = 0x03;

    private final BlockStore blockStore = new SemuxBlockStore(this);
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    protected enum StatsType {
        FORGED, HIT, MISSED
    }

    private final List<BlockchainListener> listeners = new ArrayList<>();
    private final Config config;
    private final Genesis genesis;

    private Database indexDB;
    private Database blockDB;

    private AccountState accountState;
    private DelegateState delegateState;

    private Block latestBlock;

    private ActivatedForks forks;

    public BlockchainImpl(Config config, DatabaseFactory dbFactory) {
        this(config, Genesis.load(config.network()), dbFactory);
    }

    public BlockchainImpl(Config config, Genesis genesis, DatabaseFactory dbFactory) {
        this.config = config;
        this.genesis = genesis;
        openDb(config, dbFactory);
    }

    private synchronized void openDb(Config config, DatabaseFactory dbFactory) {
        // upgrade if possible
        upgradeDatabase(config, dbFactory);

        this.indexDB = dbFactory.getDB(DatabaseName.INDEX);
        this.blockDB = dbFactory.getDB(DatabaseName.BLOCK);

        this.accountState = new AccountStateImpl(dbFactory.getDB(DatabaseName.ACCOUNT));
        this.delegateState = new DelegateStateImpl(this, dbFactory.getDB(DatabaseName.DELEGATE),
                dbFactory.getDB(DatabaseName.VOTE));

        // checks if the database needs to be initialized
        byte[] number = indexDB.get(Bytes.of(TYPE_LATEST_BLOCK_NUMBER));

        // load the activate forks from database
        forks = new ActivatedForks(this, config, getActivatedForks());

        if (number == null || number.length == 0) {
            // initialize the database for the first time
            initializeDb();
        } else {
            // load the latest block
            latestBlock = getBlock(Bytes.toLong(number));
        }
    }

    private void initializeDb() {
        // initialize database version
        indexDB.put(Bytes.of(TYPE_DATABASE_VERSION), Bytes.of(DATABASE_VERSION));

        // pre-allocation
        for (Premine p : genesis.getPremines().values()) {
            accountState.adjustAvailable(p.getAddress(), p.getAmount());
        }
        accountState.commit();

        // delegates
        for (Entry<String, byte[]> e : genesis.getDelegates().entrySet()) {
            delegateState.register(e.getValue(), Bytes.of(e.getKey()), 0);
        }
        delegateState.commit();

        // add block
        addBlock(genesis);
    }

    @Override
    public AccountState getAccountState() {
        return accountState;
    }

    @Override
    public DelegateState getDelegateState() {
        return delegateState;
    }

    @Override
    public Block getLatestBlock() {
        return latestBlock;
    }

    @Override
    public long getLatestBlockNumber() {
        return latestBlock.getNumber();
    }

    @Override
    public byte[] getLatestBlockHash() {
        return latestBlock.getHash();
    }

    @Override
    public long getBlockNumber(byte[] hash) {
        byte[] number = indexDB.get(Bytes.merge(TYPE_BLOCK_NUMBER_BY_HASH, hash));
        return (number == null) ? -1 : Bytes.toLong(number);
    }

    @Override
    public Block getBlock(long number) {
        return getBlock(blockDB, number, false);
    }

    @Override
    public Block getBlock(byte[] hash) {
        long number = getBlockNumber(hash);
        return (number == -1) ? null : getBlock(number);
    }

    @Override
    public BlockHeader getBlockHeader(long number) {
        byte[] header = blockDB.get(Bytes.merge(TYPE_BLOCK_HEADER_BY_NUMBER, Bytes.of(number)));
        return (header == null) ? null : BlockHeader.fromBytes(header);
    }

    @Override
    public BlockHeader getBlockHeader(byte[] hash) {
        long number = getBlockNumber(hash);
        return (number == -1) ? null : getBlockHeader(number);
    }

    @Override
    public boolean hasBlock(long number) {
        return blockDB.get(Bytes.merge(TYPE_BLOCK_HEADER_BY_NUMBER, Bytes.of(number))) != null;
    }

    private static class TransactionIndex {
        long blockNumber;
        int transactionOffset;
        int resultOffset;

        public TransactionIndex(long blockNumber, int transactionOffset, int resultOffset) {
            this.blockNumber = blockNumber;
            this.transactionOffset = transactionOffset;
            this.resultOffset = resultOffset;
        }

        public byte[] toBytes() {
            SimpleEncoder enc = new SimpleEncoder();
            enc.writeLong(blockNumber);
            enc.writeInt(transactionOffset);
            enc.writeInt(resultOffset);
            return enc.toBytes();
        }

        public static TransactionIndex fromBytes(byte[] bytes) {
            SimpleDecoder dec = new SimpleDecoder(bytes);
            long number = dec.readLong();
            int transactionOffset = dec.readInt();
            int resultOffset = dec.readInt();
            return new TransactionIndex(number, transactionOffset, resultOffset);
        }
    }

    @Override
    public Transaction getTransaction(byte[] hash) {
        byte[] bytes = indexDB.get(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, hash));
        if (bytes != null) {
            // coinbase transaction
            if (bytes.length > 64) {
                return Transaction.fromBytes(bytes);
            }

            TransactionIndex index = TransactionIndex.fromBytes(bytes);
            byte[] transactions = blockDB
                    .get(Bytes.merge(TYPE_BLOCK_TRANSACTIONS_BY_NUMBER, Bytes.of(index.blockNumber)));
            SimpleDecoder dec = new SimpleDecoder(transactions, index.transactionOffset);
            return Transaction.fromBytes(dec.readBytes());
        }

        return null;
    }

    @Override
    public Transaction getCoinbaseTransaction(long blockNumber) {
        return blockNumber == 0
                ? null
                : getTransaction(indexDB.get(Bytes.merge(TYPE_BLOCK_COINBASE_BY_NUMBER, Bytes.of(blockNumber))));
    }

    @Override
    public boolean hasTransaction(final byte[] hash) {
        return indexDB.get(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, hash)) != null;
    }

    @Override
    public TransactionResult getTransactionResult(byte[] hash) {
        byte[] bytes = indexDB.get(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, hash));
        if (bytes != null) {
            // coinbase transaction
            if (bytes.length > 64) {
                return new TransactionResult();
            }

            TransactionIndex index = TransactionIndex.fromBytes(bytes);
            byte[] results = blockDB.get(Bytes.merge(TYPE_BLOCK_RESULTS_BY_NUMBER, Bytes.of(index.blockNumber)));
            SimpleDecoder dec = new SimpleDecoder(results, index.resultOffset);
            return TransactionResult.fromBytes(dec.readBytes());
        }

        return null;
    }

    @Override
    public long getTransactionBlockNumber(byte[] hash) {
        Transaction tx = getTransaction(hash);
        if (tx.getType() == TransactionType.COINBASE) {
            return tx.getNonce();
        }

        byte[] bytes = indexDB.get(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, hash));
        if (bytes != null) {
            SimpleDecoder dec = new SimpleDecoder(bytes);
            return dec.readLong();
        }

        return -1;
    }

    @Override
    public synchronized void addBlock(Block block) {
        long number = block.getNumber();
        byte[] hash = block.getHash();

        if (number != genesis.getNumber() && number != latestBlock.getNumber() + 1) {
            logger.error("Adding wrong block: number = {}, expected = {}", number, latestBlock.getNumber() + 1);
            throw new BlockchainException("Blocks can only be added sequentially");
        }

        // [1] update block
        blockDB.put(Bytes.merge(TYPE_BLOCK_HEADER_BY_NUMBER, Bytes.of(number)), block.getEncodedHeader());
        blockDB.put(Bytes.merge(TYPE_BLOCK_TRANSACTIONS_BY_NUMBER, Bytes.of(number)), block.getEncodedTransactions());
        blockDB.put(Bytes.merge(TYPE_BLOCK_RESULTS_BY_NUMBER, Bytes.of(number)), block.getEncodedResults());
        blockDB.put(Bytes.merge(TYPE_BLOCK_VOTES_BY_NUMBER, Bytes.of(number)), block.getEncodedVotes());

        indexDB.put(Bytes.merge(TYPE_BLOCK_NUMBER_BY_HASH, hash), Bytes.of(number));

        // [2] update transaction indices
        List<Transaction> txs = block.getTransactions();
        Pair<byte[], List<Integer>> transactionIndices = block.getEncodedTransactionsAndIndices();
        Pair<byte[], List<Integer>> resultIndices = block.getEncodedResultsAndIndices();
        Amount reward = Block.getBlockReward(block, config);

        for (int i = 0; i < txs.size(); i++) {
            Transaction tx = txs.get(i);
            TransactionResult result = block.getResults().get(i);

            TransactionIndex index = new TransactionIndex(number, transactionIndices.getRight().get(i),
                    resultIndices.getRight().get(i));
            indexDB.put(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, tx.getHash()), index.toBytes());

            // [3] update transaction_by_account index
            addTransactionToAccount(tx, tx.getFrom());
            if (!Arrays.equals(tx.getFrom(), tx.getTo())) {
                addTransactionToAccount(tx, tx.getTo());
            }

            // index internal transactions
            for (SemuxInternalTransaction internalTx : result.getInternalTransactions()) {
                addInternalTransactionToAccount(tx, internalTx, internalTx.getFrom());
                if (!Arrays.equals(internalTx.getFrom(), internalTx.getTo())) {
                    addInternalTransactionToAccount(tx, internalTx, internalTx.getTo());
                }
            }
        }

        if (number != genesis.getNumber()) {
            // [4] coinbase transaction
            Transaction tx = new Transaction(config.network(),
                    TransactionType.COINBASE,
                    block.getCoinbase(),
                    reward,
                    Amount.ZERO,
                    block.getNumber(),
                    block.getTimestamp(),
                    Bytes.EMPTY_BYTES);
            tx.sign(Constants.COINBASE_KEY);
            indexDB.put(Bytes.merge(TYPE_TRANSACTION_INDEX_BY_HASH, tx.getHash()), tx.toBytes());
            indexDB.put(Bytes.merge(TYPE_BLOCK_COINBASE_BY_NUMBER, Bytes.of(block.getNumber())), tx.getHash());
            addTransactionToAccount(tx, block.getCoinbase());

            // [5] update validator statistics
            List<String> validators = getValidators();
            String primary = config.spec().getPrimaryValidator(validators, number, 0,
                    this.isForkActivated(UNIFORM_DISTRIBUTION));
            adjustValidatorStats(block.getCoinbase(), StatsType.FORGED, 1);
            if (primary.equals(Hex.encode(block.getCoinbase()))) {
                adjustValidatorStats(Hex.decode0x(primary), StatsType.HIT, 1);
            } else {
                adjustValidatorStats(Hex.decode0x(primary), StatsType.MISSED, 1);
            }
        }

        // [6] update validator set
        if (number % config.spec().getValidatorUpdateInterval() == 0) {
            updateValidators(block.getNumber());
        }

        // [7] update latest_block
        latestBlock = block;
        indexDB.put(Bytes.of(TYPE_LATEST_BLOCK_NUMBER), Bytes.of(number));

        for (BlockchainListener listener : listeners) {
            listener.onBlockAdded(block);
        }

        activateForks();
    }

    @Override
    public Genesis getGenesis() {
        return genesis;
    }

    @Override
    public void addListener(BlockchainListener listener) {
        listeners.add(listener);
    }

    @Override
    public int getTransactionCount(byte[] address) {
        byte[] cnt = indexDB.get(Bytes.merge(TYPE_TRANSACTION_COUNT_BY_ADDRESS, address));
        return (cnt == null) ? 0 : Bytes.toInt(cnt);
    }

    @Override
    public List<Transaction> getTransactions(byte[] address, int from, int to) {
        List<Transaction> list = new ArrayList<>();

        int total = getTransactionCount(address);
        for (int i = from; i < total && i < to; i++) {
            byte[] key = getNthTransactionIndexKey(address, i);
            byte[] value = indexDB.get(key);
            list.add(getTransaction(value));
        }

        return list;
    }

    @Override
    public List<String> getValidators() {
        List<String> validators = new ArrayList<>();

        byte[] v = indexDB.get(Bytes.of(TYPE_VALIDATORS));
        if (v != null) {
            SimpleDecoder dec = new SimpleDecoder(v);
            int n = dec.readInt();
            for (int i = 0; i < n; i++) {
                validators.add(dec.readString());
            }
        }

        return validators;
    }

    @Override
    public ValidatorStats getValidatorStats(byte[] address) {
        byte[] key = Bytes.merge(TYPE_VALIDATOR_STATS_BY_ADDRESS, address);
        byte[] value = indexDB.get(key);

        return (value == null) ? new ValidatorStats(0, 0, 0) : ValidatorStats.fromBytes(value);
    }

    /**
     * Updates the validator set.
     *
     * @param number
     */
    public void updateValidators(long number) {
        List<String> validators = new ArrayList<>();

        List<Delegate> delegates = delegateState.getDelegates();
        int max = Math.min(delegates.size(), config.spec().getNumberOfValidators(number));
        for (int i = 0; i < max; i++) {
            Delegate d = delegates.get(i);
            validators.add(Hex.encode(d.getAddress()));
        }

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(validators.size());
        for (String v : validators) {
            enc.writeString(v);
        }
        indexDB.put(Bytes.of(TYPE_VALIDATORS), enc.toBytes());
    }

    /**
     * Adjusts validator statistics.
     *
     * @param address
     *            validator address
     * @param type
     *            stats type
     * @param delta
     *            difference
     */
    protected void adjustValidatorStats(byte[] address, StatsType type, long delta) {
        byte[] key = Bytes.merge(TYPE_VALIDATOR_STATS_BY_ADDRESS, address);
        byte[] value = indexDB.get(key);

        ValidatorStats stats = (value == null) ? new ValidatorStats(0, 0, 0) : ValidatorStats.fromBytes(value);

        switch (type) {
        case FORGED:
            stats.setBlocksForged(stats.getBlocksForged() + delta);
            break;
        case HIT:
            stats.setTurnsHit(stats.getTurnsHit() + delta);
            break;
        case MISSED:
            stats.setTurnsMissed(stats.getTurnsMissed() + delta);
            break;
        default:
            break;
        }

        indexDB.put(key, stats.toBytes());
    }

    /**
     * Sets the total number of transaction of an account.
     *
     * @param address
     * @param total
     */
    protected void setTransactionCount(byte[] address, int total) {
        indexDB.put(Bytes.merge(TYPE_TRANSACTION_COUNT_BY_ADDRESS, address), Bytes.of(total));
    }

    /**
     * Adds a transaction to an account.
     *
     * @param tx
     * @param address
     */
    protected void addTransactionToAccount(Transaction tx, byte[] address) {
        int total = getTransactionCount(address);
        indexDB.put(getNthTransactionIndexKey(address, total), tx.getHash());
        setTransactionCount(address, total + 1);
    }

    /**
     * Returns the N-th transaction index key of an account.
     *
     * @param address
     * @param n
     * @return
     */
    protected byte[] getNthTransactionIndexKey(byte[] address, int n) {
        return Bytes.merge(Bytes.of(TYPE_TRANSACTION_HASH_BY_ADDRESS_AND_INDEX), address, Bytes.of(n));
    }

    @Override
    public int getInternalTransactionCount(byte[] address) {
        byte[] cnt = indexDB.get(Bytes.merge(TYPE_INTERNAL_TRANSACTION_COUNT_BY_ADDRESS, address));
        return (cnt == null) ? 0 : Bytes.toInt(cnt);
    }

    @Override
    public List<SemuxInternalTransaction> getInternalTransactions(byte[] address, int from, int to) {
        List<SemuxInternalTransaction> list = new ArrayList<>();

        int total = getInternalTransactionCount(address);
        for (int i = from; i < total && i < to; i++) {
            byte[] key = getNthInternalTransactionIndexKey(address, i);
            byte[] value = indexDB.get(key);
            list.add(SemuxInternalTransaction.fromBytes(value));
        }

        return list;
    }

    /**
     * Sets the total number of internal transaction of an account.
     *
     * @param address
     * @param total
     */
    protected void setInternalTransactionCount(byte[] address, int total) {
        indexDB.put(Bytes.merge(TYPE_INTERNAL_TRANSACTION_COUNT_BY_ADDRESS, address), Bytes.of(total));
    }

    /**
     * Adds an internal transaction to an account.
     *
     * @param tx
     * @param address
     */
    protected void addInternalTransactionToAccount(Transaction root, SemuxInternalTransaction tx, byte[] address) {
        int total = getInternalTransactionCount(address);
        indexDB.put(getNthInternalTransactionIndexKey(address, total), tx.toBytes());
        setInternalTransactionCount(address, total + 1);
    }

    /**
     * Returns the N-th internal transaction index key of an account.
     *
     * @param address
     * @param n
     * @return
     */
    protected byte[] getNthInternalTransactionIndexKey(byte[] address, int n) {
        return Bytes.merge(Bytes.of(TYPE_INTERNAL_TRANSACTION_BY_ADDRESS_AND_INDEX), address, Bytes.of(n));
    }

    /**
     * Returns the version of current database.
     *
     * @return
     */
    protected int getDatabaseVersion() {
        return getDatabaseVersion(indexDB);
    }

    /**
     * Validator statistics.
     */
    public static class ValidatorStats {
        private long blocksForged;
        private long turnsHit;
        private long turnsMissed;

        public ValidatorStats(long forged, long hit, long missed) {
            this.blocksForged = forged;
            this.turnsHit = hit;
            this.turnsMissed = missed;
        }

        public long getBlocksForged() {
            return blocksForged;
        }

        void setBlocksForged(long forged) {
            this.blocksForged = forged;
        }

        public long getTurnsHit() {
            return turnsHit;
        }

        void setTurnsHit(long hit) {
            this.turnsHit = hit;
        }

        public long getTurnsMissed() {
            return turnsMissed;
        }

        void setTurnsMissed(long missed) {
            this.turnsMissed = missed;
        }

        public byte[] toBytes() {
            SimpleEncoder enc = new SimpleEncoder();
            enc.writeLong(blocksForged);
            enc.writeLong(turnsHit);
            enc.writeLong(turnsMissed);
            return enc.toBytes();
        }

        public static ValidatorStats fromBytes(byte[] bytes) {
            SimpleDecoder dec = new SimpleDecoder(bytes);
            long forged = dec.readLong();
            long hit = dec.readLong();
            long missed = dec.readLong();
            return new ValidatorStats(forged, hit, missed);
        }
    }

    @Override
    public boolean isForkActivated(Fork fork, long number) {
        return forks.isActivated(fork, number);
    }

    @Override
    public boolean isForkActivated(Fork fork) {
        // the latest block has been imported, we should check the
        // fork status at latest_block + 1.
        return forks.isActivated(fork, getLatestBlockNumber() + 1);
    }

    @Override
    public boolean isVMEnabled() {
        return isForkActivated(Fork.VIRTUAL_MACHINE);
    }

    @Override
    public boolean isVotingPrecompiledUpgraded() {
        return isForkActivated(VOTING_PRECOMPILED_UPGRADE);
    }

    @Override
    public boolean isEd25519ContractEnabled() {
        return isForkActivated(ED25519_CONTRACT);
    }

    @Override
    public byte[] constructBlockHeaderDataField() {
        Set<Fork> set = new HashSet<>();

        if (config.forkUniformDistributionEnabled()) {
            addFork(set, UNIFORM_DISTRIBUTION);
        }

        if (config.forkVirtualMachineEnabled()) {
            addFork(set, VIRTUAL_MACHINE);
        }

        if (config.forkVotingPrecompiledUpgradeEnabled()) {
            addFork(set, VOTING_PRECOMPILED_UPGRADE);
        }
        if (config.forkEd25519ContractEnabled()) {
            addFork(set, ED25519_CONTRACT);
        }

        return set.isEmpty() ? new BlockHeaderData().toBytes() : new BlockHeaderData(ForkSignalSet.of(set)).toBytes();
    }

    private void addFork(Set<Fork> set, Fork fork) {
        long[] period = config.spec().getForkSignalingPeriod(fork);
        long number = getLatestBlockNumber() + 1;

        if (/* !this.isForkActivated(fork) && */number >= period[0] && number <= period[1]) {
            set.add(fork);
        }
    }

    @Override
    public ReentrantReadWriteLock getStateLock() {
        return stateLock;
    }

    @Override
    public boolean importBlock(Block block, boolean validateVotes) {
        AccountState asTrack = this.getAccountState().track();
        DelegateState dsTrack = this.getDelegateState().track();
        return validateBlock(block, asTrack, dsTrack, validateVotes) && applyBlock(block, asTrack, dsTrack);
    }

    /**
     * Validate the block. Votes are validated only if validateVotes is true.
     *
     * @param block
     * @param asTrack
     * @param dsTrack
     * @param validateVotes
     * @return
     */
    protected boolean validateBlock(Block block, AccountState asTrack, DelegateState dsTrack, boolean validateVotes) {
        try {
            BlockHeader header = block.getHeader();
            List<Transaction> transactions = block.getTransactions();

            // [1] check block header
            Block latest = this.getLatestBlock();
            if (!block.validateHeader(header, latest.getHeader())) {
                logger.error("Invalid block header");
                return false;
            }

            // [?] additional checks by block importer
            // - check points
            if (config.checkpoints().containsKey(header.getNumber()) &&
                    !Arrays.equals(header.getHash(), config.checkpoints().get(header.getNumber()))) {
                logger.error("Checkpoint validation failed, checkpoint is {} => {}, getting {}", header.getNumber(),
                        Hex.encode0x(config.checkpoints().get(header.getNumber())),
                        Hex.encode0x(header.getHash()));
                return false;
            }

            // [2] check transactions
            if (!block.validateTransactions(header, transactions, config.network())) {
                logger.error("Invalid transactions");
                return false;
            }
            if (transactions.stream().anyMatch(tx -> this.hasTransaction(tx.getHash()))) {
                logger.error("Duplicated transaction hash is not allowed");
                return false;
            }

            // [3] evaluate transactions
            TransactionExecutor transactionExecutor = new TransactionExecutor(config, blockStore, isVMEnabled(),
                    isVotingPrecompiledUpgraded(), isEd25519ContractEnabled());
            List<TransactionResult> results = transactionExecutor.execute(transactions, asTrack, dsTrack,
                    new SemuxBlock(block.getHeader(), config.spec().maxBlockGasLimit()),
                    0);
            if (!block.validateResults(header, results)) {
                logger.error("Invalid transaction results");
                return false;
            }
            block.setResults(results); // overwrite the results

            // [4] evaluate votes
            if (validateVotes) {
                return validateBlockVotes(block);
            }

            return true;
        } catch (Exception e) {
            logger.error("Unexpected exception during block validation", e);
            return false;
        }
    }

    @Override
    public boolean validateBlockVotes(Block block) {
        int maxValidators = config.spec().getNumberOfValidators(block.getNumber());

        List<String> validatorList = this.getValidators();

        if (validatorList.size() > maxValidators) {
            validatorList = validatorList.subList(0, maxValidators);
        }
        Set<String> validators = new HashSet<>(validatorList);

        int twoThirds = (int) Math.ceil(validators.size() * 2.0 / 3.0);

        Vote vote = new Vote(VoteType.PRECOMMIT, Vote.VALUE_APPROVE, block.getNumber(), block.getView(),
                block.getHash());
        byte[] encoded = vote.getEncoded();

        // check validity of votes
        if (block.getVotes().stream().anyMatch(sig -> !validators.contains(Hex.encode(sig.getAddress())))) {
            logger.warn("Block votes are invalid");
            return false;
        }

        if (!Key.isVerifyBatchSupported()) {
            if (!block.getVotes().stream()
                    .allMatch(sig -> Key.verify(encoded, sig))) {
                logger.warn("Block votes are invalid");
                return false;
            }
        } else {
            if (!Key.verifyBatch(Collections.nCopies(block.getVotes().size(), encoded), block.getVotes())) {
                logger.warn("Block votes are invalid");
                return false;
            }
        }

        // at least two thirds voters
        if (block.getVotes().stream()
                .map(sig -> new ByteArray(sig.getA()))
                .collect(Collectors.toSet()).size() < twoThirds) {
            logger.warn("Not enough votes, required (2/3+) = {}, actual = {}", twoThirds, block.getVotes().size());
            return false;
        }

        return true;
    }

    protected boolean applyBlock(Block block, AccountState asTrack, DelegateState dsTrack) {
        // [5] apply block reward and tx fees
        Amount reward = Block.getBlockReward(block, config);

        if (reward.isPositive()) {
            asTrack.adjustAvailable(block.getCoinbase(), reward);
        }

        // [6] commit the updates
        asTrack.commit();
        dsTrack.commit();

        ReentrantReadWriteLock.WriteLock writeLock = this.stateLock.writeLock();
        writeLock.lock();
        try {
            // [7] flush state to disk
            this.getAccountState().commit();
            this.getDelegateState().commit();

            // [8] add block to chain
            this.addBlock(block);
        } finally {
            writeLock.unlock();
        }

        return true;
    }

    /**
     * Attempt to activate pending forks at current height.
     */
    protected void activateForks() {
        if (config.forkUniformDistributionEnabled()
                && forks.activateFork(UNIFORM_DISTRIBUTION)) {
            setActivatedForks(forks.getActivatedForks());
        }
        if (config.forkVirtualMachineEnabled()
                && forks.activateFork(VIRTUAL_MACHINE)) {
            setActivatedForks(forks.getActivatedForks());
        }
        if (config.forkVotingPrecompiledUpgradeEnabled()
                && forks.activateFork(VOTING_PRECOMPILED_UPGRADE)) {
            setActivatedForks(forks.getActivatedForks());
        }
        if (config.forkEd25519ContractEnabled()
                && forks.activateFork(ED25519_CONTRACT)) {
            setActivatedForks(forks.getActivatedForks());
        }
    }

    /**
     * Returns the set of active forks.
     *
     * @return
     */
    protected Map<Fork, Fork.Activation> getActivatedForks() {
        Map<Fork, Fork.Activation> activations = new HashMap<>();
        byte[] value = indexDB.get(Bytes.of(TYPE_ACTIVATED_FORKS));
        if (value != null) {
            SimpleDecoder simpleDecoder = new SimpleDecoder(value);
            final int numberOfForks = simpleDecoder.readInt();
            for (int i = 0; i < numberOfForks; i++) {
                Fork.Activation activation = Fork.Activation.fromBytes(simpleDecoder.readBytes());
                activations.put(activation.fork, activation);
            }
        }
        return activations;
    }

    /**
     * Sets the set of activate forks.
     *
     * @return
     */
    protected void setActivatedForks(Map<Fork, Fork.Activation> activatedForks) {
        SimpleEncoder simpleEncoder = new SimpleEncoder();
        simpleEncoder.writeInt(activatedForks.size());
        for (Entry<Fork, Fork.Activation> entry : activatedForks.entrySet()) {
            simpleEncoder.writeBytes(entry.getValue().toBytes());
        }
        indexDB.put(Bytes.of(TYPE_ACTIVATED_FORKS), simpleEncoder.toBytes());
    }

    private static void upgradeDatabase(Config config, DatabaseFactory dbFactory) {
        if (getLatestBlockNumber(dbFactory.getDB(DatabaseName.INDEX)) != null
                && getDatabaseVersion(dbFactory.getDB(DatabaseName.INDEX)) < BlockchainImpl.DATABASE_VERSION) {
            upgrade(config, dbFactory, Long.MAX_VALUE);
        }
    }

    public static void upgrade(Config config, DatabaseFactory dbFactory, long to) {
        try {
            logger.info("Upgrading the database... DO NOT CLOSE THE WALLET!");
            Instant begin = Instant.now();

            Path dataDir = dbFactory.getDataDir();
            String dataDirName = dataDir.getFileName().toString();

            // setup temp chain
            Path tempPath = dataDir.resolveSibling(dataDirName + "-temp");
            delete(tempPath);
            LeveldbDatabase.LeveldbFactory tempDbFactory = new LeveldbDatabase.LeveldbFactory(tempPath.toFile());
            BlockchainImpl tempChain = new BlockchainImpl(config, tempDbFactory);

            // import all blocks
            long imported = 0;
            Database indexDB = dbFactory.getDB(DatabaseName.INDEX);
            Database blockDB = dbFactory.getDB(DatabaseName.BLOCK);
            byte[] bytes = getLatestBlockNumber(indexDB);
            long latestBlockNumber = (bytes == null) ? 0 : Bytes.toLong(bytes);
            long target = Math.min(latestBlockNumber, to);
            for (long i = 1; i <= target; i++) {
                boolean result = tempChain.importBlock(getBlock(blockDB, i, true), false);
                if (!result) {
                    break;
                }

                if (i % 1000 == 0) {
                    PubSubFactory.getDefault().publish(new BlockchainDatabaseUpgradingEvent(i, latestBlockNumber));
                    logger.info("Loaded {} / {} blocks", i, target);
                }
                imported++;
            }

            // close both database factory
            dbFactory.close();
            tempDbFactory.close();

            // swap the database folders
            Path backupPath = dataDir.resolveSibling(dataDirName + "-backup");
            dbFactory.moveTo(backupPath);
            tempDbFactory.moveTo(dataDir);
            delete(backupPath); // delete old database to save space.

            Instant end = Instant.now();
            logger.info("Database upgraded: found blocks = {}, imported = {}, took = {}", latestBlockNumber, imported,
                    TimeUtil.formatDuration(Duration.between(begin, end)));
        } catch (IOException e) {
            logger.error("Failed to upgrade database", e);
        }
    }

    // THE FOLLOWING TYPE ID SHOULD NEVER CHANGE

    private static Block getBlock(Database blockDB, long number, boolean skipResults) {
        byte[] header = blockDB.get(Bytes.merge(TYPE_BLOCK_HEADER_BY_NUMBER, Bytes.of(number)));
        byte[] transactions = blockDB.get(Bytes.merge(TYPE_BLOCK_TRANSACTIONS_BY_NUMBER, Bytes.of(number)));
        byte[] results = skipResults ? null : blockDB.get(Bytes.merge(TYPE_BLOCK_RESULTS_BY_NUMBER, Bytes.of(number)));
        byte[] votes = blockDB.get(Bytes.merge(TYPE_BLOCK_VOTES_BY_NUMBER, Bytes.of(number)));

        return (header == null) ? null : Block.fromComponents(header, transactions, results, votes);
    }

    private static byte[] getLatestBlockNumber(Database indexDB) {
        return indexDB.get(Bytes.of(TYPE_LATEST_BLOCK_NUMBER));
    }

    private static int getDatabaseVersion(Database indexDB) {
        byte[] version = indexDB.get(Bytes.of(TYPE_DATABASE_VERSION));
        return version == null ? 0 : Bytes.toInt(version);
    }

    private static void delete(Path directory) throws IOException {
        if (!directory.toFile().exists()) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
