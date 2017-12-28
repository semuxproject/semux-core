/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.semux.core.TransactionType;
import org.semux.net.msg.MessageCode;

/**
 * Describes the blockchain configurations.
 */
public interface Config {

    // =========================
    // General
    // =========================
    /**
     * Returns the data directory.
     * 
     * @return
     */
    File dataDir();

    /**
     * Returns the network id.
     * 
     * @return
     */
    byte networkId();

    /**
     * Returns the network id.
     * 
     * @return
     */
    short networkVersion();

    /**
     * Returns the max total size of all transactions in a block, encoding overhead
     * not counted.
     * 
     * @return
     */
    int maxBlockTransactionsSize();

    /**
     * Returns the max data size for the given transaction type.
     * 
     * @return
     */
    int maxTransactionDataSize(TransactionType type);

    /**
     * Returns the min transaction fee.
     * 
     * @return
     */
    long minTransactionFee();

    /**
     * Returns the min amount of value burned when registering as a delegate.
     * 
     * @return
     */
    long minDelegateBurnAmount();

    /**
     * Returns the block number before which this client needs to be upgraded.
     * 
     * @return
     */
    long mandatoryUpgrade();

    /**
     * Returns the block reward for a specific block.
     * 
     * @param number
     *            block number
     * @return the block reward
     */
    long getBlockReward(long number);

    /**
     * Returns the validator update rate.
     * 
     * @return
     */
    long getValidatorUpdateInterval();

    /**
     * Returns the number of validators.
     * 
     * @param number
     * @return
     */
    int getNumberOfValidators(long number);

    /**
     * Returns the primary validator for a specific [height, view].
     * 
     * @param validators
     * @param height
     * @param view
     * @return
     */
    String getPrimaryValidator(List<String> validators, long height, int view);

    /**
     * Returns the client id.
     * 
     * @return
     */
    String getClientId();

    // =========================
    // P2P
    // =========================

    /**
     * Returns the declared IP address.
     */
    Optional<String> p2pDeclaredIp();

    /**
     * Returns the p2p listening IP address.
     * 
     * @return
     */
    String p2pListenIp();

    /**
     * Returns the p2p listening port.
     * 
     * @return
     */
    int p2pListenPort();

    /**
     * Returns a set of seed nodes for P2P.
     * 
     * @return
     */
    Set<InetSocketAddress> p2pSeedNodes();

    // =========================
    // Network
    // =========================
    /**
     * Returns the max number of outbound connections.
     * 
     * @return
     */
    int netMaxOutboundConnections();

    /**
     * Returns the max number of inbound connections.
     * 
     * @return
     */
    int netMaxInboundConnections();

    /**
     * Returns the max message queue size.
     * 
     * @return
     */
    int netMaxMessageQueueSize();

    /**
     * Returns the max size of frame body, in bytes.
     * 
     * @return
     */
    int netMaxFrameBodySize();

    /**
     * Returns the max size of packet, in bytes.
     * 
     * @return
     */
    int netMaxPacketSize();

    /**
     * Returns the message broadcast redundancy.
     * 
     * @return
     */
    int netRelayRedundancy();

    /**
     * Returns the handshake expire time in milliseconds.
     * 
     * @return
     */
    int netHandshakeExpiry();

    /**
     * Returns the channel idle timeout.
     * 
     * @return
     */
    int netChannelIdleTimeout();

    /**
     * Returns a set of prioritized messages.
     * 
     * @return
     */
    Set<MessageCode> netPrioritizedMessages();

    // =========================
    // API
    // =========================

    /**
     * Returns whether API is enabled.
     * 
     * @return
     */
    boolean apiEnabled();

    /**
     * Returns the API listening IP address.
     * 
     * @return
     */
    String apiListenIp();

    /**
     * Returns the API listening port.
     * 
     * @return
     */
    int apiListenPort();

    /**
     * Returns the user name for API basic authentication.
     * 
     * @return
     */
    String apiUsername();

    /**
     * Returns the password for API basic authentication.
     * 
     * @return
     */
    String apiPassword();

    // =========================
    // BFT consensus
    // =========================

    /**
     * Returns the duration of NEW_HEIGHT state. This allows validators to catch up.
     * 
     * @return
     */
    long bftNewHeightTimeout();

    /**
     * Returns the duration of PROPOSE state.
     * 
     * @return
     */
    long bftProposeTimeout();

    /**
     * Returns the duration of VALIDATE state.
     * 
     * @return
     */
    long bftValidateTimeout();

    /**
     * Return the duration of PRE_COMMIT state.
     * 
     * @return
     */
    long bftPreCommitTimeout();

    /**
     * Return the duration of COMMIT state. May be skipped after +2/3 commit votes.
     * 
     * @return
     */
    long bftCommitTimeout();

    /**
     * Return the duration of FINALIZE state. This allows validators to persist
     * block.
     * 
     * @return
     */
    long bftFinalizeTimeout();

    // =========================
    // Virtual machine
    // =========================

    /**
     * Returns whether virtual machine is enabled.
     * 
     * @return
     */
    boolean vmEnabled();

    /**
     * Returns the max size of process stack in words.
     * 
     * @return
     */
    int vmMaxStackSize();

    /**
     * Returns the initial size of process heap in bytes.
     * 
     * @return
     */
    int vmInitialHeapSize();
}
