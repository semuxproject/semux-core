package org.semux.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.semux.core.Unit;
import org.semux.crypto.Hex;
import org.semux.utils.Bytes;
import org.semux.utils.ClosableIterator;

public class DBScanner {

    public static void scanVote() {
        LevelDB db = new LevelDB(DBName.VOTE);
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();

            System.out.println(Hex.encode(e.getKey()) + ": " + Hex.encode(e.getValue()));
        }
        itr.close();
    }

    public static void scanBalance() {
        Map<String, Long> balances = new HashMap<>();

        LevelDB db = new LevelDB(DBName.ACCOUNT);
        ClosableIterator<Entry<byte[], byte[]>> itr = db.iterator();
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();

            if (e.getKey().length == 21 && e.getKey()[20] <= 1) {
                String address = Hex.encode(e.getKey()).substring(0, 40);
                long value = Bytes.toLong(e.getValue()) / Unit.SEM;
                balances.put(address, balances.containsKey(address) ? balances.get(address) + value : value);
            }
        }
        itr.close();

        db = new LevelDB(DBName.DELEGATE);
        itr = db.iterator();
        while (itr.hasNext()) {
            Entry<byte[], byte[]> e = itr.next();

            if (e.getKey().length == 20) {
                String address = Hex.encode(e.getKey());
                long value = 1000;
                balances.put(address, balances.containsKey(address) ? balances.get(address) + value : value);
            }
        }
        itr.close();

        List<String> accounts = new ArrayList<>(balances.keySet());
        accounts.sort((o1, o2) -> {
            return Long.compare(balances.get(o2), balances.get(o1));
        });

        long total = 0;
        for (int i = 4; i < accounts.size(); i++) {
            String a = accounts.get(i);
            total += balances.get(a);
            System.out.println(a + ": " + balances.get(a));
        }
        System.out.println("Total: " + total + " SEM");
    }

    public static void main(String[] args) {
        scanBalance();
    }

}
