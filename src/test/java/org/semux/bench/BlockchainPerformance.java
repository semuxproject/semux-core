package org.semux.bench;

import org.semux.core.Block;
import org.semux.core.Blockchain;
import org.semux.core.BlockchainImpl;
import org.semux.db.DBFactory;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;

public class BlockchainPerformance {

    public static void blockDbSizes() {
        DBFactory dbFactory = new DBFactory() {
            private final KVDB indexDB = new LevelDB(DBName.INDEX);
            private final KVDB blockDB = new LevelDB(DBName.BLOCK);
            private final KVDB accountDB = new LevelDB(DBName.ACCOUNT);
            private final KVDB delegateDB = new LevelDB(DBName.DELEGATE);
            private final KVDB voteDB = new LevelDB(DBName.VOTE);

            @Override
            public KVDB getDB(DBName name) {
                switch (name) {
                case INDEX:
                    return indexDB;
                case BLOCK:
                    return blockDB;
                case ACCOUNT:
                    return accountDB;
                case DELEGATE:
                    return delegateDB;
                case VOTE:
                    return voteDB;
                default:
                    throw new RuntimeException("Unexpected database: " + name);
                }
            }
        };
        Blockchain chain = new BlockchainImpl(dbFactory);

        long total = 0;
        long max = Integer.MIN_VALUE;
        long min = Integer.MAX_VALUE;
        long n = chain.getLatestBlockNumber();

        for (int i = 1; i <= n; i++) {
            Block b = chain.getBlock(i);
            int size = b.toBytes().length;
            total += size;
            max = Math.max(max, size);
            min = Math.min(min, size);
        }

        System.out.println("AVG: " + total / n);
        System.out.println("MAX: " + max);
        System.out.println("MIN:" + min);
    }

    public static void main(String[] args) {
        blockDbSizes();
    }
}
