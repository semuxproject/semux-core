/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.config;

import org.semux.Network;
import org.semux.config.exception.ConfigException;
import org.semux.core.Amount;
import org.semux.core.TransactionType;
import org.semux.crypto.Hash;
import org.semux.net.CapabilitySet;
import org.semux.net.NodeManager.Node;
import org.semux.net.msg.MessageCode;
import org.semux.util.BigIntegerUtil;
import org.semux.util.Bytes;
import org.semux.util.StringUtil;
import org.semux.util.SystemUtil;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.semux.core.Amount.Unit.MILLI_SEM;
import static org.semux.core.Amount.Unit.SEM;
import static org.semux.core.Amount.ZERO;

public abstract class AbstractConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    private static final String CONFIG_FILE = "semux.properties";

    // =========================
    // General
    // =========================
    protected File dataDir;
    protected Network network;
    protected short networkVersion;

    protected int maxBlockTransactionsSize = 1024 * 1024;
    protected Amount minTransactionFee = MILLI_SEM.of(5);
    protected long maxTransactionTimeDrift = TimeUnit.HOURS.toMillis(2);
    protected Amount minDelegateBurnAmount = SEM.of(1000);

    // =========================
    // P2P
    // =========================
    protected String p2pDeclaredIp = null;
    protected String p2pListenIp = "0.0.0.0";
    protected int p2pListenPort = Constants.DEFAULT_P2P_PORT;
    protected Set<Node> p2pSeedNodes = new HashSet<>();

    // =========================
    // Network
    // =========================
    protected int netMaxOutboundConnections = 128;
    protected int netMaxInboundConnections = 512;
    protected int netMaxInboundConnectionsPerIp = 5;
    protected int netMaxMessageQueueSize = 4096;
    protected int netMaxFrameBodySize = 128 * 1024;
    protected int netMaxPacketSize = 8 * 1024 * 1024;
    protected int netRelayRedundancy = 16;
    protected int netHandshakeExpiry = 5 * 60 * 1000;
    protected int netChannelIdleTimeout = 2 * 60 * 1000;
    protected Set<MessageCode> netPrioritizedMessages = new HashSet<>(Arrays.asList(
            MessageCode.BFT_NEW_HEIGHT,
            MessageCode.BFT_NEW_VIEW,
            MessageCode.BFT_PROPOSAL,
            MessageCode.BFT_VOTE));
    protected List<String> netDnsSeedsMainNet = Collections
            .unmodifiableList(Arrays.asList("mainnet.semux.org", "mainnet.semux.net"));
    protected List<String> netDnsSeedsTestNet = Collections.singletonList("testnet.semux.org");

    // =========================
    // Sync
    // =========================
    protected long downloadTimeout = 2000L;
    protected int maxQueuedJobs = 8192;
    protected int maxPendingJobs = 256;
    protected int maxPendingBlocks = 512;

    // =========================
    // API
    // =========================
    protected boolean apiEnabled = false;
    protected String apiListenIp = "127.0.0.1";
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
    protected long maxBlockTimeDrift = TimeUnit.SECONDS.toMillis(30);

    // =========================
    // Virtual machine
    // =========================
    protected int vmMaxStackSize = 1024;
    protected int vmInitHeapSize = 128;
    protected int vmBlockGasLimit = 999_999;
    protected int vmMaxBlockGasLimit = 9_999_999;
    protected int vmMinGasPrice = 1;

    // =========================
    // UI
    // =========================
    protected Locale locale = Locale.getDefault();
    protected String uiUnit = "SEM";
    protected int uiFractionDigits = 9;

    // =========================
    // Forks
    // =========================
    protected boolean forkUniformDistributionEnabled = false;
    protected boolean forkVirtualMachineEnabled = false;

    /**
     * Create an {@link AbstractConfig} instance.
     *
     * @param dataDir
     * @param network
     * @param networkVersion
     */
    protected AbstractConfig(String dataDir, Network network, short networkVersion) {
        this.dataDir = new File(dataDir);
        this.network = network;
        this.networkVersion = networkVersion;

        init();
        validate();
    }

    @Override
    public File getFile() {
        return new File(configDir(), CONFIG_FILE);
    }

    @Override
    public Amount getBlockReward(long number) {
        if (number <= 10_000_000L) {
            return SEM.of(3);
        } else if (number <= 25_000_000L) {
            return SEM.of(2);
        } else if (number <= 40_000_000L) {
            return SEM.of(1);
        } else {
            return ZERO;
        }
    }

    @Override
    public long getValidatorUpdateInterval() {
        return 200L;
    }

    @Override
    public int getNumberOfValidators(long number) {
        long step = 2L * 60L * 2L; // two hours

        if (number < 84L * step) {
            return 16 + (int) (number / step);
        } else {
            return 100;
        }
    }

    @Override
    public String getPrimaryValidator(List<String> validators, long height, int view, boolean uniformDist) {
        if (uniformDist) {
            return validators.get(getUniformDistPrimaryValidatorNumber(validators.size(), height, view));
        } else {
            byte[] key = Bytes.merge(Bytes.of(height), Bytes.of(view));
            return validators.get((Hash.h256(key)[0] & 0xff) % validators.size());
        }
    }

    private int getUniformDistPrimaryValidatorNumber(int size, long height, long view) {
        // use round-robin for view 0
        if (view == 0) {
            return (int) (height % (long) size);
        }

        // here we ensure there will never be consecutive block forgers after view
        // change
        int deterministicRand;
        final int prevDeterministicRand = getUniformDistPrimaryValidatorNumber(size, height, view - 1);
        BigInteger subView = BigInteger.ZERO;
        do {
            BigInteger seed = BigIntegerUtil
                    .random(BigInteger.valueOf(height))
                    .xor(BigIntegerUtil.random(BigInteger.valueOf(view)))
                    .add(subView);
            deterministicRand = BigIntegerUtil
                    .random(seed)
                    .mod(BigInteger.valueOf(size))
                    .intValue();
            subView = subView.add(BigInteger.ONE);
        } while (deterministicRand == prevDeterministicRand && size > 1);

        return deterministicRand;
    }

    @Override
    public String getClientId() {
        return String.format("%s/v%s-%s/%s/%s",
                Constants.CLIENT_NAME,
                Constants.CLIENT_VERSION,
                SystemUtil.getImplementationVersion(),
                SystemUtil.getOsName().toString(),
                SystemUtil.getOsArch());
    }

    @Override
    public CapabilitySet getClientCapabilities() {
        return Constants.CLIENT_CAPABILITIES;
    }

    @Override
    public File dataDir() {
        return dataDir;
    }

    @Override
    public File databaseDir() {
        return databaseDir(network);
    }

    @Override
    public File databaseDir(Network network) {
        return new File(dataDir, Constants.DATABASE_DIR + File.separator + network.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public File configDir() {
        return new File(dataDir, Constants.CONFIG_DIR);
    }

    @Override
    public Network network() {
        return network;
    }

    @Override
    public short networkVersion() {
        return networkVersion;
    }

    @Override
    public int maxBlockTransactionsSize() {
        return maxBlockTransactionsSize;
    }

    @Override
    public int maxTransactionDataSize(TransactionType type) {
        switch (type) {
        case COINBASE:
            return 0; // not required

        case TRANSFER:
            return 128; // for memo

        case DELEGATE:
            return 16; // for name

        case UNVOTE:
        case VOTE:
            return 0; // not required

        case CREATE:
        case CALL:
            return 64 * 1024; // for dapps

        default:
            throw new UnreachableException();
        }
    }

    @Override
    public Amount minTransactionFee() {
        return minTransactionFee;
    }

    @Override
    public long maxTransactionTimeDrift() {
        return maxTransactionTimeDrift;
    }

    @Override
    public Amount minDelegateBurnAmount() {
        return minDelegateBurnAmount;
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
    public Set<Node> p2pSeedNodes() {
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
    public int netMaxInboundConnectionsPerIp() {
        return netMaxInboundConnectionsPerIp;
    }

    @Override
    public int netMaxMessageQueueSize() {
        return netMaxMessageQueueSize;
    }

    @Override
    public int netMaxFrameBodySize() {
        return netMaxFrameBodySize;
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
    public List<String> netDnsSeedsMainNet() {
        return netDnsSeedsMainNet;
    }

    @Override
    public List<String> netDnsSeedsTestNet() {
        return netDnsSeedsTestNet;
    }

    @Override
    public long syncDownloadTimeout() {
        return downloadTimeout;
    }

    @Override
    public int syncMaxQueuedJobs() {
        return maxQueuedJobs;
    }

    @Override
    public int syncMaxPendingJobs() {
        return maxPendingJobs;
    }

    @Override
    public int syncMaxPendingBlocks() {
        return maxPendingBlocks;
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
    public String apiUsername() {
        return apiUsername == null ? "admin" : apiUsername;
    }

    @Override
    public String apiPassword() {
        return apiPassword == null ? "admin" : apiPassword;
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
    public long maxBlockTimeDrift() {
        return maxBlockTimeDrift;
    }

    @Override
    public int vmMaxStackSize() {
        return vmMaxStackSize;
    }

    @Override
    public int vmInitialHeapSize() {
        return vmInitHeapSize;
    }

    @Override
    public int vmBlockGasLimit() {
        return vmBlockGasLimit;
    }

    @Override
    public int vmMaxBlockGasLimit() {
        return vmMaxBlockGasLimit;
    }

    @Override
    public int vmMinGasPrice() {
        return vmMinGasPrice;
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public String uiUnit() {
        return uiUnit;
    }

    @Override
    public int uiFractionDigits() {
        return uiFractionDigits;
    }

    @Override
    public boolean forkUniformDistributionEnabled() {
        return forkUniformDistributionEnabled;
    }

    @Override
    public boolean forkVirtualMachineEnabled() {
        return forkVirtualMachineEnabled;
    }

    protected void init() {
        File f = getFile();
        if (!f.exists()) {
            // exit if the config file does not exist
            return;
        }

        try (FileInputStream in = new FileInputStream(f)) {

            Properties props = new Properties();
            props.load(in);

            for (Object k : props.keySet()) {
                String name = (String) k;

                switch (name) {
                case "p2p.declaredIp":
                    p2pDeclaredIp = props.getProperty(name).trim();
                    break;
                case "p2p.listenIp":
                    p2pListenIp = props.getProperty(name).trim();
                    break;
                case "p2p.listenPort":
                    p2pListenPort = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "p2p.seedNodes":
                    String[] nodes = props.getProperty(name).trim().split(",");
                    for (String node : nodes) {
                        if (!node.trim().isEmpty()) {
                            String[] tokens = node.trim().split(":");
                            if (tokens.length == 2) {
                                p2pSeedNodes.add(new Node(tokens[0], Integer.parseInt(tokens[1])));
                            } else {
                                p2pSeedNodes.add(new Node(tokens[0], Constants.DEFAULT_P2P_PORT));
                            }
                        }
                    }
                    break;

                case "net.maxInboundConnections":
                    netMaxInboundConnections = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.maxInboundConnectionsPerIp":
                    netMaxInboundConnectionsPerIp = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.maxOutboundConnections":
                    netMaxOutboundConnections = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.maxMessageQueueSize":
                    netMaxMessageQueueSize = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.relayRedundancy":
                    netRelayRedundancy = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.channelIdleTimeout":
                    netChannelIdleTimeout = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "net.dnsSeeds.mainNet":
                    netDnsSeedsMainNet = Arrays.asList(props.getProperty(name).trim().split(","));
                    break;
                case "net.dnsSeeds.testNet":
                    netDnsSeedsTestNet = Arrays.asList(props.getProperty(name).trim().split(","));
                    break;

                case "sync.downloadTimeout":
                    downloadTimeout = Long.parseLong(props.getProperty(name).trim());
                    break;
                case "sync.maxQueuedJobs":
                    maxQueuedJobs = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "sync.maxPendingJobs":
                    maxPendingJobs = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "sync.maxPendingBlocks":
                    maxPendingBlocks = Integer.parseInt(props.getProperty(name).trim());
                    break;

                case "api.enabled":
                    apiEnabled = Boolean.parseBoolean(props.getProperty(name).trim());
                    break;
                case "api.listenIp":
                    apiListenIp = props.getProperty(name).trim();
                    break;
                case "api.listenPort":
                    apiListenPort = Integer.parseInt(props.getProperty(name).trim());
                    break;
                case "api.username":
                    apiUsername = props.getProperty(name).trim();
                    break;
                case "api.password":
                    apiPassword = props.getProperty(name).trim();
                    break;
                case "ui.locale": {
                    // ui.locale must be in format of en_US ([language]_[country])
                    String[] localeComponents = props.getProperty(name).trim().split("_");
                    if (localeComponents.length == 2) {
                        locale = new Locale(localeComponents[0], localeComponents[1]);
                    }
                    break;
                }
                case "ui.unit": {
                    uiUnit = props.getProperty(name).trim();
                    break;
                }
                case "ui.fractionDigits": {
                    uiFractionDigits = Integer.parseInt(props.getProperty(name).trim());
                    break;
                }
                case "vm.blockGasLimit": {
                    vmBlockGasLimit = Integer.parseInt(props.getProperty(name).trim());
                    break;
                }
                case "vm.minGasPrice": {
                    vmMinGasPrice = Integer.parseInt(props.getProperty(name).trim());
                    break;
                }
                default:
                    logger.error("Unsupported option: {} = {}", name, props.getProperty(name));
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load config file: {}", f, e);
        }
    }

    private void validate() {
        if (apiEnabled &&
                ("YOUR_API_USERNAME".equals(apiUsername) || "YOUR_API_PASSWORD".equals(apiPassword))) {
            throw new ConfigException("Please change your API username/password from the default values.");
        }
    }
}
