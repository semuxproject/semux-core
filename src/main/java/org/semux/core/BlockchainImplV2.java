/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import static org.semux.core.Fork.UNIFORM_DISTRIBUTION;
import static org.semux.core.Fork.VIRTUAL_MACHINE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.core.exception.BlockchainException;
import org.semux.core.state.AccountState;
import org.semux.core.state.AccountStateImplV2;
import org.semux.core.state.Delegate;
import org.semux.core.state.DelegateState;
import org.semux.core.state.DelegateStateImplV2;
import org.semux.crypto.Hex;
import org.semux.db.Batch;
import org.semux.db.BatchManager;
import org.semux.db.BatchName;
import org.semux.db.BatchOperation;
import org.semux.db.Database;
import org.semux.db.DatabasePrefixesV2;
import org.semux.util.ByteArray;
import org.semux.util.Bytes;
import org.semux.util.SimpleDecoder;
import org.semux.util.SimpleEncoder;
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
public class BlockchainImplV2 implements Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainImplV2.class);

    private final List<BlockchainListener> listeners = new ArrayList<>();
    private final Config config;
    private final Genesis genesis;

    private final Database blockDB;
    private final BatchManager batchManager;
    private final AccountState accountState;
    private final DelegateState delegateState;

    private Block latestBlock;
    private ActivatedForks forks;

    private BlockCodec blockCodec;

    public BlockchainImplV2(Config config, Genesis genesis, Database blockDB, BatchManager batchManager,
            BlockCodec blockCodec) {
        this.config = config;
        this.genesis = genesis;
        this.blockDB = blockDB;
        this.batchManager = batchManager;
        this.blockCodec = blockCodec;
        this.accountState = new AccountStateImplV2(blockDB, batchManager);
        this.delegateState = new DelegateStateImplV2(this, blockDB, batchManager);
        forks = new ActivatedForks((Blockchain) Proxy.newProxyInstance(
                BlockchainImplV2.class.getClassLoader(),
                new Class[] { Blockchain.class },
                new PendingChainHandler(this)), config, getActivatedForks());
        latestBlock = getBlock(Bytes.toLong(blockDB.get(Bytes.of(DatabasePrefixesV2.TYPE_LATEST_BLOCK_NUMBER))));
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
        byte[] number = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HASH, hash));
        return (number == null) ? -1 : Bytes.toLong(number);
    }

    @Override
    public Block getBlock(long number) {
        byte[] header = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HEADER, Bytes.of(number)));
        byte[] transactions = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_TRANSACTIONS, Bytes.of(number)));
        byte[] results = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_RESULTS, Bytes.of(number)));
        byte[] votes = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_VOTES, Bytes.of(number)));

        return (header == null) ? null : blockCodec.decoder().fromComponents(header, transactions, results, votes);
    }

    @Override
    public Block getBlock(byte[] hash) {
        long number = getBlockNumber(hash);
        return (number == -1) ? null : getBlock(number);
    }

    @Override
    public BlockHeader getBlockHeader(long number) {
        byte[] header = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HEADER, Bytes.of(number)));
        return (header == null) ? null : BlockHeader.fromBytes(header);
    }

    @Override
    public BlockHeader getBlockHeader(byte[] hash) {
        long number = getBlockNumber(hash);
        return (number == -1) ? null : getBlockHeader(number);
    }

    @Override
    public boolean hasBlock(long number) {
        return blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HEADER, Bytes.of(number))) != null;
    }

    @Override
    public Transaction getTransaction(byte[] hash) {
        byte[] bytes = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, hash));
        if (bytes != null) {
            // coinbase transaction
            if (bytes.length > 64) {
                return Transaction.fromBytes(bytes);
            }

            SimpleDecoder dec = new SimpleDecoder(bytes);
            long number = dec.readLong();
            int start = dec.readInt();
            dec.readInt();

            byte[] transactions = blockDB
                    .get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_TRANSACTIONS, Bytes.of(number)));
            dec = new SimpleDecoder(transactions, start);
            return Transaction.fromBytes(dec.readBytes());
        }

        return null;
    }

    @Override
    public Transaction getCoinbaseTransaction(long blockNumber) {
        return blockNumber == 0
                ? null
                : getTransaction(blockDB
                        .get(Bytes.merge(DatabasePrefixesV2.TYPE_COINBASE_TRANSACTION_HASH, Bytes.of(blockNumber))));
    }

    @Override
    public boolean hasTransaction(final byte[] hash) {
        return blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, hash)) != null;
    }

    @Override
    public TransactionResult getTransactionResult(byte[] hash) {
        byte[] bytes = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, hash));
        if (bytes != null) {
            // coinbase transaction
            if (bytes.length > 64) {
                return null; // no results for coinbase transaction
            }

            SimpleDecoder dec = new SimpleDecoder(bytes);
            long number = dec.readLong();
            dec.readInt();
            int start = dec.readInt();

            byte[] results = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_RESULTS, Bytes.of(number)));
            dec = new SimpleDecoder(results, start);
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

        byte[] bytes = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, hash));
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
        Batch batch = batchManager.getBatchInstance(BatchName.ADD_BLOCK);
        batch.add(
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HEADER, Bytes.of(number)),
                        blockCodec.encoder().getEncodedHeader(block)),
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_TRANSACTIONS, Bytes.of(number)),
                        blockCodec.encoder().getEncodedTransactions(block)),
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_RESULTS, Bytes.of(number)),
                        blockCodec.encoder().getEncodedResults(block)),
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_VOTES, Bytes.of(number)),
                        blockCodec.encoder().getEncodedVotes(block)),
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HASH, hash), Bytes.of(number)));

        // [2] update transaction indices
        List<Transaction> txs = block.getTransactions();
        Pair<byte[], List<Integer>> transactionIndices = blockCodec.encoder().getEncodedTransactionsAndIndices(block);
        Pair<byte[], List<Integer>> resultIndices = blockCodec.encoder().getEncodedTransactionsAndIndices(block);
        Amount reward = Block.getBlockReward(block, config);

        for (int i = 0; i < txs.size(); i++) {
            Transaction tx = txs.get(i);

            SimpleEncoder enc = new SimpleEncoder();
            enc.writeLong(number);
            enc.writeInt(transactionIndices.getRight().get(i));
            enc.writeInt(resultIndices.getRight().get(i));

            batch.add(BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, tx.getHash()),
                    enc.toBytes()));

            // [3] update transaction_by_account index
            addTransactionToAccount(tx, tx.getFrom());
            if (!Arrays.equals(tx.getFrom(), tx.getTo())) {
                addTransactionToAccount(tx, tx.getTo());
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
            batch.add(
                    BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_TRANSACTION_HASH, tx.getHash()),
                            tx.toBytes()),
                    BatchOperation.put(
                            Bytes.merge(DatabasePrefixesV2.TYPE_COINBASE_TRANSACTION_HASH, Bytes.of(block.getNumber())),
                            tx.getHash()));
            addTransactionToAccount(tx, block.getCoinbase());

            // [5] update validator statistics
            List<String> validators = getValidators();
            String primary = config.getPrimaryValidator(validators, number, 0, forks.isActivated(UNIFORM_DISTRIBUTION));
            adjustValidatorStats(block.getCoinbase(), ValidatorStatsType.FORGED, 1);
            if (primary.equals(Hex.encode(block.getCoinbase()))) {
                adjustValidatorStats(Hex.decode0x(primary), ValidatorStatsType.HIT, 1);
            } else {
                adjustValidatorStats(Hex.decode0x(primary), ValidatorStatsType.MISSED, 1);
            }
        }

        // [6] update validator set
        if (number % config.getValidatorUpdateInterval() == 0) {
            updateValidators(block.getNumber());
        }

        // [7] update latest_block
        latestBlock = block;
        batch.add(BatchOperation.put(Bytes.of(DatabasePrefixesV2.TYPE_LATEST_BLOCK_NUMBER), Bytes.of(number)));

        activateForks(number + 1);
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
        byte[] cnt = blockDB.get(Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT_TRANSACTION, address));
        return (cnt == null) ? 0 : Bytes.toInt(cnt);
    }

    @Override
    public List<Transaction> getTransactions(byte[] address, int from, int to) {
        List<Transaction> list = new ArrayList<>();

        int total = getTransactionCount(address);
        for (int i = from; i < total && i < to; i++) {
            byte[] key = getNthTransactionIndexKey(address, i);
            byte[] value = blockDB.get(key);
            list.add(getTransaction(value));
        }

        return list;
    }

    @Override
    public List<String> getValidators() {
        List<String> validators = new ArrayList<>();

        byte[] v = blockDB.get(Bytes.of(DatabasePrefixesV2.TYPE_VALIDATORS));
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
        byte[] key = Bytes.merge(DatabasePrefixesV2.TYPE_VALIDATOR_STATS, address);
        byte[] value = blockDB.get(key);

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
        int max = Math.min(delegates.size(), config.getNumberOfValidators(number));
        for (int i = 0; i < max; i++) {
            Delegate d = delegates.get(i);
            validators.add(Hex.encode(d.getAddress()));
        }

        SimpleEncoder enc = new SimpleEncoder();
        enc.writeInt(validators.size());
        for (String v : validators) {
            enc.writeString(v);
        }
        batchManager.getBatchInstance(BatchName.ADD_BLOCK)
                .add(BatchOperation.put(Bytes.of(DatabasePrefixesV2.TYPE_VALIDATORS), enc.toBytes()));
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
    protected void adjustValidatorStats(byte[] address, ValidatorStatsType type, long delta) {
        byte[] key = Bytes.merge(DatabasePrefixesV2.TYPE_VALIDATOR_STATS, address);
        byte[] value = blockDB.get(key);

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

        batchManager.getBatchInstance(BatchName.ADD_BLOCK).add(
                BatchOperation.put(key, stats.toBytes()));
    }

    /**
     * Sets the total number of transaction of an account.
     *
     * @param address
     * @param total
     */
    protected void setTransactionCount(byte[] address, int total) {
        batchManager.getBatchInstance(BatchName.ADD_BLOCK).add(
                BatchOperation.put(Bytes.merge(DatabasePrefixesV2.TYPE_ACCOUNT_TRANSACTION, address), Bytes.of(total)));
    }

    /**
     * Adds a transaction to an account.
     *
     * @param tx
     * @param address
     */
    protected void addTransactionToAccount(Transaction tx, byte[] address) {
        int total = getTransactionCount(address);
        batchManager.getBatchInstance(BatchName.ADD_BLOCK).add(
                BatchOperation.put(getNthTransactionIndexKey(address, total), tx.getHash()));
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
        return Bytes.merge(Bytes.of(DatabasePrefixesV2.TYPE_ACCOUNT_TRANSACTION), address, Bytes.of(n));
    }

    @Override
    public boolean isForkActivated(Fork fork) {
        return forks.isActivated(fork);
    }

    @Override
    public boolean isForkActivated(Fork fork, long height) {
        return forks.isActivated(fork, height);
    }

    @Override
    public byte[] constructBlockData() {
        Set<Fork> set = new HashSet<>();
        if (config.forkUniformDistributionEnabled()
                && !forks.isActivated(UNIFORM_DISTRIBUTION)
                && latestBlock.getNumber() + 1 <= UNIFORM_DISTRIBUTION.activationDeadline) {
            set.add(UNIFORM_DISTRIBUTION);
        }

        /**
         * For prior forks, if a validator did not update, their node would stop syncing
         * at point of fork. However, VM will only stop syncing at point a smart
         * contract is created.
         *
         * Because of this, we need to keep signalling until activation deadline, rather
         * than short circuiting (or until all nodes are shown to be updated).
         */
        if (config.forkVirtualMachineEnabled()
                // && !forks.isActivated(VIRTUAL_MACHINE)
                && latestBlock.getNumber() + 1 <= VIRTUAL_MACHINE.activationDeadline) {
            set.add(VIRTUAL_MACHINE);
        }

        return set.isEmpty() ? new byte[0]
                : BlockHeaderData.v1(new BlockHeaderData.ForkSignalSet(set.toArray(new Fork[0]))).toBytes();
    }

    /**
     * Attempt to activate pending forks at current height.
     */
    protected void activateForks(long height) {
        if (config.forkUniformDistributionEnabled()
                && forks.activateFork(UNIFORM_DISTRIBUTION, height)) {
            setActivatedForks(forks.getActivatedForks());
        }
        if (config.forkVirtualMachineEnabled()
                && forks.activateFork(VIRTUAL_MACHINE, height)) {
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
        byte[] value = blockDB.get(Bytes.of(DatabasePrefixesV2.TYPE_ACTIVATED_FORKS));
        if (value != null) {
            SimpleDecoder simpleDecoder = new SimpleDecoder(value);
            final int numberOfForks = simpleDecoder.readInt();
            for (int i = 0; i < numberOfForks; i++) {
                Fork.Activation activation = Fork.Activation.fromBytes(simpleDecoder.readBytes());
                activations.put(activation.fork, activation);
            }
        }

        if (!activations.isEmpty()) {
            logger.info("Activated Forks");
            activations.forEach((fork, activation) -> logger.info(fork.name + " @ block " + activation.activatedAt));
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
        batchManager.getBatchInstance(BatchName.ADD_BLOCK).add(
                BatchOperation.put(Bytes.of(DatabasePrefixesV2.TYPE_ACTIVATED_FORKS), simpleEncoder.toBytes()));
    }

    @Override
    synchronized public void commit() {
        batchManager.commit(batchManager.getBatchInstance(BatchName.ADD_BLOCK));

        Block latestBlock = getLatestBlock();
        for (BlockchainListener listener : listeners) {
            listener.onBlockAdded(latestBlock);
        }
    }

    class PendingChainHandler implements InvocationHandler {
        private final BlockchainImplV2 original;

        PendingChainHandler(BlockchainImplV2 original) {
            this.original = original;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws IllegalAccessException, IllegalArgumentException,
                InvocationTargetException {

            if (method.getName().equals("getBlockHeader")) {
                return getBlockHeader((long) args[0]);
            }

            return method.invoke(original, args);
        }

        BlockHeader getBlockHeader(long number) {
            Batch batch = batchManager.getBatchInstance(BatchName.ADD_BLOCK);
            ByteArray key = ByteArray.of(Bytes.merge(DatabasePrefixesV2.TYPE_BLOCK_HEADER, Bytes.of(number)));

            if (batch.containsPendingState(key)) {
                byte[] pendingValue = batch.lookupPendingState(key);
                if (pendingValue != null) {
                    return BlockHeader.fromBytes(pendingValue);
                } else {
                    return null;
                }
            }

            return original.getBlockHeader(number);
        }
    }
}
