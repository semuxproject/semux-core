/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.vm;

import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.TransactionResult;
import org.semux.core.state.AccountState;
import org.semux.db.MemoryDB;
import org.semux.util.Bytes;

public class SemuxRuntimeMock implements SemuxRuntime {

    public byte[] address;
    public byte[] sender;
    public long value;
    public byte[] data;

    public byte[] blockHash;
    public long blockNumber;
    public byte[] blockCoinbase;
    public long blockTimestamp;

    public AccountState accountState;
    public TransactionResult result;

    public SemuxRuntimeMock() {
        this.address = Bytes.random(20);
        this.sender = Bytes.random(20);
        this.value = 24;
        this.data = Bytes.random(4);

        this.blockHash = Bytes.random(32);
        this.blockNumber = 48;
        this.blockCoinbase = Bytes.random(20);
        this.blockTimestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000;

        Blockchain chain = new BlockchainImpl(MemoryDB.FACTORY);
        this.accountState = chain.getAccountState();
        this.result = new TransactionResult(true);
    }

    @Override
    public byte[] getAddress() {
        return address;
    }

    @Override
    public byte[] getSender() {
        return sender;
    }

    @Override
    public long getValue() {
        return value;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public byte[] getBockHash() {
        return blockHash;
    }

    @Override
    public long getBlockNumber() {
        return blockNumber;
    }

    @Override
    public byte[] getBlockCoinbase() {
        return blockCoinbase;
    }

    @Override
    public long getBlockTimestamp() {
        return blockTimestamp;
    }

    @Override
    public AccountState getAccountState() {
        return accountState;
    }

    @Override
    public TransactionResult result() {
        return result;
    }

    @Override
    public void send(byte[] to, long value, byte[] data) {
        // TODO simulate send operation
    }

}
