package info.kgeorgiy.ja.churakova.i18n.exceptions;

public class UnexpectedFormatException extends RuntimeException {
    public UnexpectedFormatException(String message) {
        super(message);
    }

    public UnexpectedFormatException(String message, Exception e) {
        super(message, e);
    }
}
