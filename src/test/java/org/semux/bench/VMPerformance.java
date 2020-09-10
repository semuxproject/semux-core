/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import static org.semux.core.Unit.SEM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.semux.Network;
import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.UnitTestnetConfig;
import org.semux.core.Amount;
import org.semux.core.Block;
import org.semux.core.BlockHeader;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.core.Fork;
import org.semux.core.Transaction;
import org.semux.core.TransactionResult;
import org.semux.core.TransactionType;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.rules.TemporaryDatabaseRule;
import org.semux.util.Bytes;
import org.semux.util.MerkleUtil;
import org.semux.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VMPerformance {
    private static final Logger logger = LoggerFactory.getLogger(VMPerformance.class);

    private static Config config = new UnitTestnetConfig(Constants.DEFAULT_DATA_DIR) {
        @Override
        public Map<Fork, Long> manuallyActivatedForks() {
            return Collections.singletonMap(Fork.VIRTUAL_MACHINE, 0L);
        }
    };
    private static Key key = new Key();

    public static void main(String[] args) throws Throwable {
        TemporaryDatabaseRule temporaryDbRule = new TemporaryDatabaseRule();
        temporaryDbRule.before();
        Blockchain blockchain = new BlockchainImpl(config, temporaryDbRule);

        // https://github.com/ensdomains/solsha1
        byte[] contractAddress = Bytes.random(20);
        byte[] contractCode = Hex.decode(
                "60806040526004361061003b576000357c0100000000000000000000000000000000000000000000000000000000900480639c438a3d14610040575b600080fd5b34801561004c57600080fd5b50610055610057565b005b60606040805190810160405280600481526020017f7465737400000000000000000000000000000000000000000000000000000000815250905061009a8161009e565b5050565b60006040518251602084019350604067ffffffffffffffc0600183011601600982820310600181146100cf576100d6565b6040820191505b50776745230100efcdab890098badcfe001032547600c3d2e1f0610131565b60008090508383101561012a5782820151905082840393506020841015610129576001846020036101000a03198082169150505b5b9392505050565b60005b8281101561053f576101478482896100f5565b85526101578460208301896100f5565b60208601526040818503106001811461016f57610178565b60808286038701535b506040830381146001811461018c5761019c565b6008850260208701511760208701525b5060405b60808110156102285760408103860151603882038701511860208203870151600c830388015118187c010000000100000001000000010000000100000001000000010000000163800000008204167ffffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffe6002830216179050808288015250600c810190506101a0565b5060805b6101408110156102b557608081038601516070820387015118604082038701516018830388015118187c030000000300000003000000030000000300000003000000030000000363400000008204167ffffffffcfffffffcfffffffcfffffffcfffffffcfffffffcfffffffcfffffffc600483021617905080828801525060188101905061022c565b508160008060005b60508110156105115760148104600081146102ef5760018114610339576002811461037657600381146103d957610412565b6501000000000085046a01000000000000000000008604189350836f01000000000000000000000000000000860416935083650100000000008604189350635a8279999250610412565b6a010000000000000000000085046f01000000000000000000000000000000860418935083650100000000008604189350636ed9eba19250610412565b6a010000000000000000000085046f01000000000000000000000000000000860417935083650100000000008604169350836a010000000000000000000086046f01000000000000000000000000000000870416179350638f1bbcdc9250610412565b6a010000000000000000000085046f0100000000000000000000000000000086041893508365010000000000860418935063ca62c1d692505b50601f770800000000000000000000000000000000000000000000008504168063ffffffe073080000000000000000000000000000000000000087041617905080840190508063ffffffff86160190508083019050807c0100000000000000000000000000000000000000000000000000000000600484028c0151040190507401000000000000000000000000000000000000000081026501000000000086041794506a0100000000000000000000633fffffff6a040000000000000000000087041663c00000006604000000000000880416170277ffffffff00ffffffff000000000000ffffffff00ffffffff8616179450506001810190506102bd565b5077ffffffff00ffffffff00ffffffff00ffffffff00ffffffff838601169450505050604081019050610134565b506c0100000000000000000000000063ffffffff821667ffffffff000000006101008404166bffffffff0000000000000000620100008504166fffffffff000000000000000000000000630100000086041673ffffffff00000000000000000000000000000000640100000000870416171717170294505050505091905056fea165627a7a72305820484b77b412a7d3ae99d173d982683437598af94409d51f12c5d9c16a1f4119160029");
        blockchain.getAccountState().setCode(contractAddress, contractCode);
        blockchain.getAccountState().adjustAvailable(key.toAddress(), Amount.of(1_000_000L, SEM));

        int numBlocks = 100;
        long t1 = TimeUtil.currentTimeMillis();
        long blockGasUsed = 0;
        for (int i = 0; i < numBlocks; i++) {
            Block bestBlock = blockchain.getLatestBlock();
            long startNonce = blockchain.getAccountState().getAccount(key.toAddress()).getNonce();

            List<Transaction> txs = new ArrayList<>();
            List<TransactionResult> res = new ArrayList<>();
            for (int j = 0; j < config.spec().maxBlockGasLimit() / 60_000L; j++) {
                Network network = config.network();
                TransactionType type = TransactionType.CALL;
                byte[] to = contractAddress;
                Amount value = Amount.ZERO;
                Amount fee = Amount.ZERO;
                long nonce = startNonce + j;
                long timestamp = TimeUtil.currentTimeMillis();
                byte[] data = Bytes.merge(Hex.decode0x("9c438a3d"), Bytes.of(j)); // sha1()
                long gas = 100_000L;
                Amount gasPrice = Amount.of(10);
                Transaction tx = new Transaction(network, type, to, value, fee, nonce, timestamp, data, gas, gasPrice)
                        .sign(key);
                txs.add(tx);
                res.add(new TransactionResult());
            }

            long number = bestBlock.getNumber() + 1;
            byte[] coinbase = key.toAddress();
            byte[] prevHash = bestBlock.getHash();
            long timestamp = bestBlock.getTimestamp() + 1;
            byte[] transactionsRoot = MerkleUtil.computeTransactionsRoot(txs);
            byte[] resultsRoot = MerkleUtil.computeResultsRoot(res);
            byte[] stateRoot = Bytes.EMPTY_HASH;
            byte[] data = {};

            BlockHeader header = new BlockHeader(number, coinbase, prevHash, timestamp, transactionsRoot, resultsRoot,
                    stateRoot, data);
            Block block = new Block(header, txs, res);

            if (!blockchain.importBlock(block, false)) {
                System.err.println("Failed to import");
                break;
            }

            if (i == 0) {
                blockGasUsed = blockchain.getBlock(number).getResults().stream()
                        .mapToLong(TransactionResult::getGasUsed).sum();
            }
        }
        long t2 = TimeUtil.currentTimeMillis();
        temporaryDbRule.after();
        logger.info("{} ms per block, {} gas consumed per block", (t2 - t1) / numBlocks, blockGasUsed);
        TimeUtil.shutdownNtpUpdater();
    }
}
