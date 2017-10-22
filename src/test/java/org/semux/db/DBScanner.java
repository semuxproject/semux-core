package org.semux.db;

import java.util.Map.Entry;

import org.semux.crypto.Hex;
import org.semux.utils.ClosableIterator;

public class DBScanner {

    public static void main(String[] args) {
        LevelDB db = new LevelDB(DBName.VOTE);
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();

            System.out.println(Hex.encode(e.getKey()) + ": " + Hex.encode(e.getValue()));
        }
        itr.close();
    }

}
