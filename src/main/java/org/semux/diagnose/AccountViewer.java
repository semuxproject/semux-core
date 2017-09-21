package org.semux.diagnose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.semux.Config;
import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;
import org.semux.utils.Bytes;
import org.semux.utils.ClosableIterator;

public class AccountViewer {
    private static Map<String, Long> map = new HashMap<>();

    private static void process(String dataDir) {
        Config.DATA_DIR = dataDir;

        KVDB db = new LevelDB(DBName.ACCOUNT);
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        try {
            while (itr.hasNext()) {
                Entry<byte[], byte[]> e = itr.next();
                byte[] k = e.getKey();
                byte[] v = e.getValue();
                if (k.length == 21 && (k[20] == 0x00 || k[20] == 0x01)) {
                    String address = Hex.encode(Arrays.copyOf(k, 20));
                    long value = Bytes.toLong(v);
                    map.put(address, map.containsKey(address) ? map.get(address) + value : value);
                }
            }
        } finally {
            if (itr != null) {
                itr.close();
            }
            db.close();
        }
    }

    public static void main(String[] args) {
        String[] dirs = { "." };
        for (String dir : dirs) {
            process(dir);
        }

        List<String> keys = new ArrayList<>(map.keySet());
        keys.sort((k1, k2) -> {
            return Long.compare(map.get(k2), map.get(k1));
        });

        for (String k : keys) {
            System.out.println(k + ": " + map.get(k) / Unit.SEM);
        }
    }

}
