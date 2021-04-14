/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.bench;

import java.io.IOException;

import org.semux.config.Config;
import org.semux.config.Constants;
import org.semux.config.MainnetConfig;
import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.db.LeveldbDatabase.LeveldbFactory;
import org.semux.net.msg.consensus.BlockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

public class CompressPerformance {

    private static final Logger logger = LoggerFactory.getLogger(CompressPerformance.class);

    private enum Mode {
        ALL_BLOCKS, BLOCKS_WITH_TX
    }

    public static void main(String[] args) throws IOException {
        Config config = new MainnetConfig(Constants.DEFAULT_ROOT_DIR);

        LeveldbFactory dbFactory = new LeveldbFactory(config.chainDir());
        Blockchain chain = new BlockchainImpl(config, dbFactory);

        for (Mode mode : Mode.values()) {
            int blocks = 0;
            int transactions = 0;
            int size = 0;
            int sizeCompressed = 0;
            long time = 0;
            for (int i = 1; i <= chain.getLatestBlockNumber(); i++) {
                Block b = chain.getBlock(i);
                BlockMessage m = new BlockMessage(b);
                if (mode == Mode.BLOCKS_WITH_TX && b.getTransactions().isEmpty()) {
                    continue;
                }

                blocks++;
                transactions += b.getTransactions().size();
                size += m.getBody().length;
                long t1 = System.nanoTime();
                sizeCompressed += Snappy.compress(m.getBody()).length;
                long t2 = System.nanoTime();
                time += t2 - t1;
            }
            logger.info("======================================");
            logger.info(mode.name());
            logger.info("======================================");
            logger.info("# of blocks      : " + blocks);
            logger.info("# of transactions: " + transactions);
            logger.info("Raw size         : " + size + " bytes");
            logger.info("Compressed size  : " + sizeCompressed + " bytes");
            logger.info("Ratio            : " + (100.0 * sizeCompressed / size) + " %");
            logger.info("Total time used  : " + time + " ns");
            logger.info("Average time used: " + time / blocks + " ns");
        }
    }
}
