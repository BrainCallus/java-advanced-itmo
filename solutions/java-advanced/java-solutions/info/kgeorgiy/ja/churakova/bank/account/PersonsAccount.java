package info.kgeorgiy.ja.churakova.bank.account;

import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.rmi.RemoteException;

/**
 * Extending {@link Account} interface for more useful interaction with {@link Person}
 *
 * @param <T> defines localization(remote or local)
 */
public interface PersonsAccount<T> extends Account {

    /**
     * Returns passport of {@link Person} owns this account
     */
    String getPersonsPassport() throws RemoteException;

    /**
     * Returns account id without {@link Person} passport
     */
    String getSelfId() throws RemoteException;
}
