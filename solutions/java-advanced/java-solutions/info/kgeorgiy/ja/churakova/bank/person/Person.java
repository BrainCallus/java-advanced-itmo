package info.kgeorgiy.ja.churakova.bank.person;

import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;
import info.kgeorgiy.ja.churakova.bank.bank.AccountOperations;

import java.rmi.RemoteException;
import java.util.Set;

/**
 * Interface for person
 *
 * @param <T> defines localization
 */
public interface Person<T> extends AccountOperations {
    /**
     * Return string representing person's passport
     *
     * @return person's passport
     * @throws RemoteException if occur
     */
    String getPassport() throws RemoteException;

    /**
     * Return string representing person's name
     *
     * @return person's name
     * @throws RemoteException if occur
     */
    String getName() throws RemoteException;

    /**
     * Return string representing person's second name
     *
     * @return person's second name
     * @throws RemoteException if occur
     */
    String getFamilyName() throws RemoteException;

    /**
     * Return all person's {@link PersonsAccount}s
     *
     * @return person's accounts
     * @throws RemoteException if occur
     */
    Set<PersonsAccount<T>> getAccounts() throws RemoteException;
}
