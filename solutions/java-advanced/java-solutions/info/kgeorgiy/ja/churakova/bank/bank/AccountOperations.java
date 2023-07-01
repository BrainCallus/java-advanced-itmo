package info.kgeorgiy.ja.churakova.bank.bank;

import info.kgeorgiy.ja.churakova.bank.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AccountOperations extends Remote {
    /**
     * Returns account by identifier.
     * @param accountId account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccountById(String accountId) throws RemoteException;

    /**
     * Creates a new account with specified identifier if it does not already exist.
     * @param accountId account id
     * @return created or existing account.
     */
    Account createAccount(String accountId) throws RemoteException;
}
