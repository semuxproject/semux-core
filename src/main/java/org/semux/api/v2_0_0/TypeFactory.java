/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v2_0_0;

import static org.semux.core.TransactionType.DELEGATE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.semux.Kernel;
import org.semux.api.v2_0_0.model.AccountType;
import org.semux.api.v2_0_0.model.AccountVoteType;
import org.semux.api.v2_0_0.model.BlockType;
import org.semux.api.v2_0_0.model.DelegateType;
import org.semux.api.v2_0_0.model.InfoType;
import org.semux.api.v2_0_0.model.PeerType;
import org.semux.api.v2_0_0.model.PendingTransactionType;
import org.semux.api.v2_0_0.model.TransactionLimitsType;
import org.semux.api.v2_0_0.model.TransactionType;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Transaction;
import org.semux.core.state.Account;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;
import org.semux.net.Peer;

public class TypeFactory {

    public static AccountType accountType(Account account, int transactionCount, int pendingTransactionCount) {
        return new AccountType()
                .address(Hex.encode0x(account.getAddress()))
                .available(encodeAmount(account.getAvailable()))
                .locked(encodeAmount(account.getLocked()))
                .nonce(String.valueOf(account.getNonce()))
                .transactionCount(transactionCount)
                .pendingTransactionCount(pendingTransactionCount);
    }

    public static BlockType blockType(Block block, Transaction coinbaseTransaction) {
        List<Transaction> txs = block.getTransactions();
        if (coinbaseTransaction != null) {
            txs.add(0, coinbaseTransaction);
        }
        return new BlockType()
                .hash(Hex.encode0x(block.getHash()))
                .number(String.valueOf(block.getNumber()))
                .view(block.getView())
                .coinbase(Hex.encode0x(block.getCoinbase()))
                .parentHash(Hex.encode0x(block.getParentHash()))
                .timestamp(String.valueOf(block.getTimestamp()))
                .transactionsRoot(Hex.encode0x(block.getTransactionsRoot()))
                .resultsRoot(Hex.encode0x(block.getResultsRoot()))
                .stateRoot(Hex.encode0x(block.getStateRoot()))
                .data(Hex.encode0x(block.getData()))
                .transactions(txs.stream()
                        .map(tx -> transactionType(block.getNumber(), tx))
                        .collect(Collectors.toList()));
    }

    public static DelegateType delegateType(BlockchainImpl.ValidatorStats validatorStats, Delegate delegate,
            boolean isValidator) {
        return new DelegateType()
                .address(Hex.encode0x(delegate.getAddress()))
                .name(delegate.getNameString())
                .registeredAt(String.valueOf(delegate.getRegisteredAt()))
                .votes(encodeAmount(delegate.getVotes()))
                .blocksForged(String.valueOf(validatorStats.getBlocksForged()))
                .turnsHit(String.valueOf(validatorStats.getTurnsHit()))
                .turnsMissed(String.valueOf(validatorStats.getTurnsMissed()))
                .validator(isValidator);
    }

    public static List<AccountVoteType> accountVotes(Blockchain blockchain, byte[] address) {
        Set<String> validators = new HashSet<>(blockchain.getValidators());
        return blockchain.getDelegateState()
                .getDelegates()
                .parallelStream()
                .map(delegate -> accountVoteType(blockchain, address, delegate,
                        validators.contains(delegate.getAddressString())))
                .filter(accountVote -> !accountVote.getVotes().equals("0"))
                .collect(Collectors.toList());
    }

    public static AccountVoteType accountVoteType(Blockchain blockchain, byte[] address, Delegate delegate,
            Boolean isValidator) {
        return new AccountVoteType()
                .delegate(
                        TypeFactory.delegateType(blockchain.getValidatorStats(delegate.getAddress()), delegate,
                                isValidator))
                .votes(
                        String.valueOf(
                                blockchain.getDelegateState().getVote(address, delegate.getAddress()).getNano()));
    }

    public static InfoType infoType(Kernel kernel) {
        return new InfoType()
                .network(InfoType.NetworkEnum.fromValue(kernel.getConfig().network().name()))
                .capabilities(kernel.getConfig().capabilitySet().toList())
                .clientId(kernel.getConfig().getClientId())
                .coinbase(Hex.encode0x(kernel.getCoinbase().toAddress()))
                .latestBlockNumber(String.valueOf(kernel.getBlockchain().getLatestBlockNumber()))
                .latestBlockHash(Hex.encode0x(kernel.getBlockchain().getLatestBlockHash()))
                .activePeers(kernel.getChannelManager().getActivePeers().size())
                .pendingTransactions(kernel.getPendingManager().getPendingTransactions().size());
    }

    public static PeerType peerType(Peer peer) {
        return new PeerType()
                .ip(peer.getIp())
                .port(peer.getPort())
                .networkVersion((int) peer.getNetworkVersion())
                .clientId(peer.getClientId())
                .peerId(Hex.PREF + peer.getPeerId())
                .latestBlockNumber(String.valueOf(peer.getLatestBlockNumber()))
                .latency(String.valueOf(peer.getLatency()))
                .capabilities(peer.getCapabilities().toList());
    }

    public static TransactionLimitsType transactionLimitsType(Kernel kernel,
            org.semux.core.TransactionType transactionType) {
        return new TransactionLimitsType()
                .maxTransactionDataSize(kernel.getConfig().maxTransactionDataSize(transactionType))
                .minTransactionFee(encodeAmount(kernel.getConfig().minTransactionFee()))
                .minDelegateBurnAmount(encodeAmount(
                        transactionType.equals(DELEGATE) ? kernel.getConfig().minDelegateBurnAmount() : null));
    }

    public static TransactionType transactionType(Long blockNumber, Transaction tx) {
        return new TransactionType()
                .blockNumber(String.valueOf(blockNumber))
                .hash(Hex.encode0x(tx.getHash()))
                .type(TransactionType.TypeEnum.fromValue(tx.getType().name()))
                .from(Hex.encode0x(tx.getFrom()))
                .to(Hex.encode0x(tx.getTo()))
                .value(encodeAmount(tx.getValue()))
                .fee(encodeAmount(tx.getFee()))
                .nonce(String.valueOf(tx.getNonce()))
                .timestamp(String.valueOf(tx.getTimestamp()))
                .data(Hex.encode0x(tx.getData()));
    }

    public static PendingTransactionType pendingTransactionType(Transaction tx) {
        return new PendingTransactionType()
                .hash(Hex.encode0x(tx.getHash()))
                .type(PendingTransactionType.TypeEnum.fromValue(tx.getType().name()))
                .from(Hex.encode0x(tx.getFrom()))
                .to(Hex.encode0x(tx.getTo()))
                .value(encodeAmount(tx.getValue()))
                .fee(encodeAmount(tx.getFee()))
                .nonce(String.valueOf(tx.getNonce()))
                .timestamp(String.valueOf(tx.getTimestamp()))
                .data(Hex.encode0x(tx.getData()));
    }

    public static String encodeAmount(Amount a) {
        return a == null ? null : String.valueOf(a.getNano());
    }
}
