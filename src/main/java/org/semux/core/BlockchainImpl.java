/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.core.state.AccountState;
import org.semux.core.state.AccountStateImpl;
import org.semux.core.state.DelegateState;
import org.semux.core.state.DelegateStateImpl;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.utils.ByteArray;
import org.semux.utils.Bytes;
import org.semux.utils.SimpleDecoder;
import org.semux.utils.SimpleEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockchainImpl implements Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainImpl.class);

    private static byte[] KEY_LATEST_BLOCK_HASH = Bytes.of("latest_block_hash");

    private KVDB indexDB;
    private KVDB blockDB;

    private AccountState accountState;
    private DelegateState delegateState;

    private Genesis genesis;
    private Block latestBlock;

    private List<BlockchainListener> listeners = new ArrayList<>();

    /**
     * Create a blockchain instance.
     * 
     * @param factory
     */
    public BlockchainImpl(DBFactory factory) {
        this.indexDB = factory.getDB(DBName.INDEX);
        this.blockDB = factory.getDB(DBName.BLOCK);

        this.accountState = new AccountStateImpl(factory.getDB(DBName.ACCOUNT));
        this.delegateState = new DelegateStateImpl(this, factory.getDB(DBName.DELEGATE), factory.getDB(DBName.VOTE));

        this.genesis = Genesis.getInstance();

        byte[] hash = indexDB.get(KEY_LATEST_BLOCK_HASH);
        if (hash == null) {
            /*
             * Update account/delegate state for the genesis block
             */
            for (Entry<ByteArray, Long> e : genesis.getPremine().entrySet()) {
                Account acc = accountState.getAccount(e.getKey().getData());
                acc.setBalance(e.getValue());
            }
            for (Entry<String, byte[]> e : genesis.getDelegates().entrySet()) {
                delegateState.register(e.getValue(), Bytes.of(e.getKey()));
            }
            accountState.commit();
            delegateState.commit();

            latestBlock = genesis;
        } else {
            latestBlock = getBlock(hash);
        }
    }

    @Override
    public AccountState getAccountState() {
        return accountState;
    }

    @Override
    public DelegateState getDeleteState() {
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
    public byte[] getBlockHash(long number) {
        if (genesis.getNumber() == number) {
            return genesis.getHash();
        }

        return indexDB.get(Bytes.of(number));
    }

    @Override
    public Block getBlock(long number) {
        if (genesis.getNumber() == number) {
            return genesis;
        }

        byte[] bytes = indexDB.get(Bytes.of(number));
        return bytes == null ? null : getBlock(bytes);
    }

    @Override
    public Block getBlock(byte[] hash) {
        if (Arrays.equals(genesis.getHash(), hash)) {
            return genesis;
        }

        byte[] bytes = blockDB.get(hash);
        return bytes == null ? null : Block.fromBytes(bytes);
    }

    @Override
    public BlockHeader getBlockHeader(long number) {
        if (genesis.getNumber() == number) {
            return genesis.getHeader();
        }

        byte[] bytes = indexDB.get(Bytes.of(number));
        return bytes == null ? null : getBlockHeader(bytes);
    }

    @Override
    public BlockHeader getBlockHeader(byte[] hash) {
        if (Arrays.equals(genesis.getHash(), hash)) {
            return genesis.getHeader();
        }

        byte[] bytes = blockDB.get(hash);
        return bytes == null ? null : BlockHeader.fromBytes(new SimpleDecoder(bytes).readBytes());
    }

    @Override
    public Transaction getTransaction(byte[] hash) {
        byte[] bytes = indexDB.get(hash);
        if (bytes != null) {
            SimpleDecoder dec = new SimpleDecoder(bytes);
            byte[] blockHash = dec.readBytes();
            int from = dec.readInt();
            int to = dec.readInt();

            byte[] block = blockDB.get(blockHash);
            return Transaction.fromBytes(Arrays.copyOfRange(block, from, to));
        }

        return null;
    }

    @Override
    public synchronized void addBlock(Block block) {

        long blockNumber = block.getNumber();
        byte[] blockHash = block.getHash();

        byte[] idx = Bytes.of(blockNumber);
        if (indexDB.get(idx) != null) {
            logger.error("Block #{} already exists", blockNumber);
            return;
        } else {
            List<Pair<Integer, Integer>> txIndices = block.getTransacitonIndexes();
            byte[] bytes = block.toBytes();

            // [1] update block
            blockDB.put(blockHash, bytes);
            indexDB.put(idx, blockHash);

            // [2] update transaction indices
            List<Transaction> txs = block.getTransactions();
            for (int i = 0; i < txs.size(); i++) {
                Transaction tx = txs.get(i);

                SimpleEncoder enc = new SimpleEncoder();
                enc.writeBytes(blockHash);
                enc.writeInt(txIndices.get(i).getLeft());
                enc.writeInt(txIndices.get(i).getRight());

                indexDB.put(tx.getHash(), enc.toBytes());

                // [3] update transaction_by_account indices
                int totalFrom = getTotalTransactions(tx.getFrom());
                int totalTo = getTotalTransactions(tx.getTo());
                indexDB.put(getNthTransactionIndexKey(tx.getFrom(), totalFrom), tx.getHash());
                indexDB.put(getNthTransactionIndexKey(tx.getTo(), totalTo), tx.getHash());
                setTotalTransactions(tx.getFrom(), totalFrom + 1);
                setTotalTransactions(tx.getTo(), totalTo + 1);
            }

            if (blockNumber > latestBlock.getNumber()) {
                latestBlock = block;

                // [4] update latest_block
                indexDB.put(KEY_LATEST_BLOCK_HASH, blockHash);
            }
        }

        for (BlockchainListener listener : listeners) {
            listener.onBlockAdded(block);
        }
    }

    /**
     * Get the number of transactions from/to an address.
     * 
     * @param address
     * @return
     */
    private int getTotalTransactions(byte[] address) {
        byte[] cnt = indexDB.get(address);
        return (cnt == null) ? 0 : Bytes.toInt(cnt);
    }

    /**
     * Set the total number of transaction of an account.
     * 
     * @param address
     * @param total
     */
    private void setTotalTransactions(byte[] address, int total) {
        indexDB.put(address, Bytes.of(total));
    }

    /**
     * Get the N-th transaction index key of an account.
     * 
     * @param address
     * @param n
     * @return
     */
    private byte[] getNthTransactionIndexKey(byte[] address, int n) {
        return Bytes.merge(address, Bytes.of(n));
    }

    @Override
    public Genesis getGenesis() {
        return genesis;
    }

    @Override
    public void addListener(BlockchainListener listener) {
        listeners.add(listener);
    }

    public List<Transaction> getTransactions(byte[] address) {
        return getTransactions(address, -1);
    }

    @Override
    public List<Transaction> getTransactions(byte[] address, int limit) {
        List<Transaction> list = new ArrayList<>();

        int total = getTotalTransactions(address);
        for (int i = total - 1; i >= 0 && (limit == -1 || list.size() < limit); i--) {
            byte[] key = getNthTransactionIndexKey(address, i);
            byte[] value = indexDB.get(key);

            list.add(getTransaction(value));
        }

        return list;
    }
}
