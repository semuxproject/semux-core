package org.semux.db.exception;

public class LevelDBException extends RuntimeException {

    public LevelDBException() {
    }

    public LevelDBException(String s) {
        super(s);
    }

    public LevelDBException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LevelDBException(Throwable throwable) {
        super(throwable);
    }

    public LevelDBException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
