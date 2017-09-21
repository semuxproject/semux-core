package org.semux.diagnose;

import java.util.Map.Entry;

import org.semux.crypto.Hex;
import org.semux.db.DBName;
import org.semux.db.KVDB;
import org.semux.db.LevelDB;
import org.semux.utils.ClosableIterator;

public class DatabaseViewer {

    public static void inspectDB(KVDB db) {
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        try {
            while (itr.hasNext()) {
                Entry<byte[], byte[]> e = itr.next();
                System.out.println(Hex.encode(e.getKey()) + "\t: " + Hex.encode(e.getValue()));
            }
        } finally {
            if (itr != null) {
                itr.close();
            }
        }
    }

    public static void inspectAll() {
        System.out.println("Databases:");
        for (DBName name : DBName.values()) {
            System.out.println("\n================\n" + name + "\n================");
            KVDB kvdb = new LevelDB(name);
            inspectDB(kvdb);
        }
    }

    public static void main(String[] args) {
        inspectAll();
    }

}
