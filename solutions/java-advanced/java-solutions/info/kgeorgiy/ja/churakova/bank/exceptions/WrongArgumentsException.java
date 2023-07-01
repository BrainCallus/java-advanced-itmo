package info.kgeorgiy.ja.churakova.bank.exceptions;

/**
 * Exception for bank operations. Thrown when operation can't be executed with given arguments
 */
public class WrongArgumentsException extends IllegalArgumentException{
    public WrongArgumentsException(String message){
        super(message);
    }
}
