/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.semux.core.Unit;
import org.semux.crypto.Hash;
import org.semux.net.msg.MessageCode;
import org.semux.util.Bytes;
import org.semux.util.StringUtil;
import org.semux.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    private static final String CONFIG_FILE = "semux.properties";

    // =========================
    // General
    // =========================
    protected File dataDir;
    protected byte networkId;
    protected short networkVersion;

    protected int maxBlockSize = 5000;
    protected long minTransactionFee = 50L * Unit.MILLI_SEM;
    protected long minDelegateFee = 1000L * Unit.SEM;
    protected long mandatoryUpgrade = Constants.BLOCKS_PER_DAY * 60L;

    // =========================
    // P2P
    // =========================
    protected String p2pDeclaredIp = null;
    protected String p2pListenIp = new InetSocketAddress(0).getAddress().getHostAddress();
    protected int p2pListenPort = Constants.DEFAULT_P2P_PORT;
    protected Set<InetSocketAddress> p2pSeedNodes = new HashSet<>();

    // =========================
    // Network
    // =========================
    protected int netMaxOutboundConnections = 128;
    protected int netMaxInboundConnections = 256;
    protected int netMaxMessageQueueSize = 4096;
    protected int netMaxFrameSize = 128 * 1024;
    protected int netMaxPacketSize = 8 * 1024 * 1024;
    protected int netRelayRedundancy = 16;
    protected int netHandshakeExpiry = 5 * 60 * 1000;
    protected int netChannelIdleTimeout = 2 * 60 * 1000;
    protected Set<MessageCode> netPrioritizedMessages = new HashSet<>(Arrays.asList( //
            MessageCode.BFT_NEW_HEIGHT, //
            MessageCode.BFT_NEW_VIEW, //
            MessageCode.BFT_PROPOSAL, //
            MessageCode.BFT_VOTE //
    ));

    // =========================
    // API
    // =========================
    protected boolean apiEnabled = false;
    protected String apiListenIp = InetAddress.getLoopbackAddress().getHostAddress();
    protected int apiListenPort = Constants.DEFAULT_API_PORT;
    protected String apiUsername = null;
    protected String apiPassword = null;

    // =========================
    // BFT consensus
    // =========================
    protected long bftNewHeightTimeout = 3000L;
    protected long bftProposeTimeout = 12000L;
    protected long bftValidateTimeout = 6000L;
    protected long bftPreCommitTimeout = 6000L;
    protected long bftCommitTimeout = 3000L;
    protected long bftFinalizeTimeout = 3000L;

    // =========================
    // Virtual machine
    // =========================
    protected boolean vmEnabled = false;
    protected int vmMaxStackSize = 1024;
    protected int vmInitHeapSize = 128;

    /**
     * Create an {@link AbstractConfig} instance.
     * 
     * @param dataDir
     * @param networkId
     * @param networkVersion
     */
    protected AbstractConfig(String dataDir, byte networkId, short networkVersion) {
        this.dataDir = new File(dataDir);
        this.networkId = networkId;
        this.networkVersion = networkVersion;

        init();
    }

    @Override
    public long getBlockReward(long number) {
        if (number <= 75_000_000L) {
            return 1 * Unit.SEM;
        } else {
            return 0;
        }
    }

    @Override
    public long getValidatorUpdateInterval() {
        return 64L * 2L;
    }

    @Override
    public int getNumberOfValidators(long number) {
        long step = 2L * 60L * 2L;

        if (number < 48L * step) {
            return 16 + (int) (number / step);
        } else {
            return 64;
        }
    }

    @Override
    public String getPrimaryValidator(List<String> validators, long height, int view) {
        byte[] key = Bytes.merge(Bytes.of(height), Bytes.of(view));
        return validators.get((Hash.h256(key)[0] & 0xff) % validators.size());
    }

    @Override
    public String getClientId() {
        return String.format("%s/v%s/%s/%s", Constants.CLIENT_NAME, Constants.CLIENT_VERSION,
                SystemUtil.getOsName().toString(), SystemUtil.getOsArch());
    }

    @Override
    public File dataDir() {
        return dataDir;
    }

    @Override
    public byte networkId() {
        return networkId;
    }

    @Override
    public short networkVersion() {
        return networkVersion;
    }

    @Override
    public long minTransactionFee() {
        return minTransactionFee;
    }

    @Override
    public long minDelegateFee() {
        return minDelegateFee;
    }

    @Override
    public int maxBlockSize() {
        return maxBlockSize;
    }

    @Override
    public long mandatoryUpgrade() {
        return mandatoryUpgrade;
    }

    @Override
    public Optional<String> p2pDeclaredIp() {
        return StringUtil.isNullOrEmpty(p2pDeclaredIp) ? Optional.empty() : Optional.of(p2pDeclaredIp);
    }

    @Override
    public String p2pListenIp() {
        return p2pListenIp;
    }

    @Override
    public int p2pListenPort() {
        return p2pListenPort;
    }

    @Override
    public Set<InetSocketAddress> p2pSeedNodes() {
        return p2pSeedNodes;
    }

    @Override
    public int netMaxOutboundConnections() {
        return netMaxOutboundConnections;
    }

    @Override
    public int netMaxInboundConnections() {
        return netMaxInboundConnections;
    }

    @Override
    public int netMaxMessageQueueSize() {
        return netMaxMessageQueueSize;
    }

    @Override
    public int netMaxFrameSize() {
        return netMaxFrameSize;
    }

    @Override
    public int netMaxPacketSize() {
        return netMaxPacketSize;
    }

    @Override
    public int netRelayRedundancy() {
        return netRelayRedundancy;
    }

    @Override
    public int netHandshakeExpiry() {
        return netHandshakeExpiry;
    }

    @Override
    public int netChannelIdleTimeout() {
        return netChannelIdleTimeout;
    }

    @Override
    public Set<MessageCode> netPrioritizedMessages() {
        return netPrioritizedMessages;
    }

    @Override
    public boolean apiEnabled() {
        return apiEnabled;
    }

    @Override
    public String apiListenIp() {
        return apiListenIp;
    }

    @Override
    public int apiListenPort() {
        return apiListenPort;
    }

    @Override
    public Optional<String> apiUsername() {
        return StringUtil.isNullOrEmpty(apiUsername) ? Optional.empty() : Optional.of(apiUsername);
    }

    @Override
    public Optional<String> apiPassword() {
        return StringUtil.isNullOrEmpty(apiPassword) ? Optional.empty() : Optional.of(apiPassword);
    }

    @Override
    public long bftNewHeightTimeout() {
        return bftNewHeightTimeout;
    }

    @Override
    public long bftProposeTimeout() {
        return bftProposeTimeout;
    }

    @Override
    public long bftValidateTimeout() {
        return bftValidateTimeout;
    }

    @Override
    public long bftPreCommitTimeout() {
        return bftPreCommitTimeout;
    }

    @Override
    public long bftCommitTimeout() {
        return bftCommitTimeout;
    }

    @Override
    public long bftFinalizeTimeout() {
        return bftFinalizeTimeout;
    }

    @Override
    public boolean vmEnabled() {
        return vmEnabled;
    }

    @Override
    public int vmMaxStackSize() {
        return vmMaxStackSize;
    }

    @Override
    public int vmInitialHeapSize() {
        return vmInitHeapSize;
    }

    protected void init() {
        File f = new File(dataDir, Constants.CONFIG_DIR + File.separator + CONFIG_FILE);
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(f)) {
            props.load(in);

            for (Object k : props.keySet()) {
                String name = (String) k;

                switch (name) {
                case "p2p.declaredIp":
                    p2pDeclaredIp = props.getProperty(name);
                    break;
                case "p2p.listenIp":
                    p2pListenIp = props.getProperty(name);
                    break;
                case "p2p.listenPort":
                    p2pListenPort = Integer.parseInt(props.getProperty(name));
                    break;
                case "p2p.seedNodes":
                    String[] nodes = props.getProperty(name).split(",");
                    for (String node : nodes) {
                        String[] tokens = node.trim().split(":");
                        if (tokens.length == 2) {
                            p2pSeedNodes.add(new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])));
                        } else {
                            p2pSeedNodes.add(new InetSocketAddress(tokens[0], Constants.DEFAULT_P2P_PORT));
                        }
                    }
                    break;

                case "net.maxInboundConnections":
                    netMaxInboundConnections = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.maxOutboundConnections":
                    netMaxInboundConnections = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.maxMessageQueueSize":
                    netMaxMessageQueueSize = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.relayRedundancy":
                    netRelayRedundancy = Integer.parseInt(props.getProperty(name));
                    break;
                case "net.channelIdleTimeout":
                    netChannelIdleTimeout = Integer.parseInt(props.getProperty(name));
                    break;

                case "api.enabled":
                    apiEnabled = Boolean.parseBoolean(props.getProperty(name));
                    break;
                case "api.listenIp":
                    apiListenIp = props.getProperty(name);
                    break;
                case "api.listenPort":
                    apiListenPort = Integer.parseInt(props.getProperty(name));
                    break;
                case "api.username":
                    apiUsername = props.getProperty(name);
                    break;
                case "api.password":
                    apiPassword = props.getProperty(name);
                    break;
                default:
                    logger.error("Unsupported option: {} = {}", name, props.getProperty(name));
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load config file: {}", f, e);
        }
    }
}
