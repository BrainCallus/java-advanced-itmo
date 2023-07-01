package info.kgeorgiy.ja.churakova.bank.exceptions;

/**
 * Exception for bank operations. Thrown when there is an attempt to do illegal operation
 */
public class BankException extends RuntimeException {
    public BankException(String message) {
        super(message);
    }
}
