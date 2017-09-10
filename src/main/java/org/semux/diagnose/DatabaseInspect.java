package org.semux.diagnose;

import java.util.Map.Entry;

import org.semux.crypto.Hex;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;
import org.semux.utils.ClosableIterator;

public class DatabaseInspect {

    public static void inspectDB(KVDB db) {
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        try {
            while (itr.hasNext()) {
                Entry<byte[], byte[]> e = itr.next();
                System.out.println(String.format("%-64s = %s", Hex.encode(e.getKey()), Hex.encode(e.getValue())));
            }
        } finally {
            if (itr != null) {
                itr.close();
            }
        }
    }

    public static void inspectAll() {
        for (DBName name : DBName.values()) {
            System.out.println("================ " + name + " ================");
            KVDB kvdb = new LevelDB(name);
            inspectDB(kvdb);
        }
    }

    public static void main(String[] args) {
        inspectAll();
    }

}
