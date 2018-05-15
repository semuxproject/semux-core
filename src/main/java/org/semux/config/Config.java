/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.semux.Network;
import org.semux.core.Amount;
import org.semux.core.TransactionType;
import org.semux.net.CapabilitySet;
import org.semux.net.NodeManager.Node;
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
     * Returns the database directory.
     *
     * @return
     */
    File databaseDir();

    /**
     * Returns the database directory.
     *
     * @return
     */
    File databaseDir(Network network);

    /**
     * Returns the config directory.
     *
     * @return
     */
    File configDir();

    /**
     * Returns the network.
     *
     * @return
     */
    Network network();

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
    Amount minTransactionFee();

    /**
     * Returns the maximum allowed time drift between transaction timestamp and
     * local clock. See ${@link org.semux.core.PendingManager#processTransaction}
     *
     * @return
     */
    long maxTransactionTimeDrift();

    /**
     * Returns the min amount of value burned when registering as a delegate.
     *
     * @return
     */
    Amount minDelegateBurnAmount();

    /**
     * Returns the block reward for a specific block.
     *
     * @param number
     *            block number
     * @return the block reward
     */
    Amount getBlockReward(long number);

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
     * @param uniformDist
     * @return
     */
    String getPrimaryValidator(List<String> validators, long height, int view, boolean uniformDist);

    /**
     * Returns the client id.
     *
     * @return
     */
    String getClientId();

    /**
     * Returns the set of capability.
     *
     * @return
     */
    CapabilitySet capabilitySet();

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
    Set<Node> p2pSeedNodes();

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
     * Returns the max number of inbound connections of each unique IP.
     *
     * @return
     */
    int netMaxInboundConnectionsPerIp();

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

    /**
     * Returns a list of DNS seeds for main network
     *
     * @return
     */
    List<String> netDnsSeedsMainNet();

    /**
     * Returns a list of DNS seeds for test network
     *
     * @return
     */
    List<String> netDnsSeedsTestNet();

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

    /**
     * Returns the maximum time drift of a block time in the future.
     *
     * @return
     */
    long maxBlockTimeDrift();

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

    // =========================
    // UI
    // =========================

    /**
     * Returns the localization of UI
     *
     * @return
     */
    Locale locale();

    /**
     * Returns the unit of displayed values.
     *
     * @return
     */
    String uiUnit();

    /**
     * Returns the fraction digits of displayed values.
     *
     * @return
     */
    int uiFractionDigits();

    // =========================
    // Forks
    // =========================

    /**
     * Returns whether UNIFORM_DISTRIBUTION fork is enabled.
     *
     * @return
     */
    boolean forkUniformDistributionEnabled();

    // =========================
    // Checkpoints
    // =========================

    /**
     * Get checkpoints.
     *
     * @return a map of blockchain checkpoints [block height] => [block hash]
     */
    Map<Long, byte[]> checkpoints();
}
