package info.kgeorgiy.ja.churakova.bank.bank;

import info.kgeorgiy.ja.churakova.bank.Localization;
import info.kgeorgiy.ja.churakova.bank.MyRemote;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.rmi.RemoteException;
import java.util.Set;

public interface Bank extends AccountOperations {
    /**
     * Find person in bank by given passport
     *
     * @param passport person's passport
     * @param type     {@link Localization} remote or local
     * @return {@link Person} founded in {@link Bank} or null, if not found
     * @throws RemoteException if occur in {@link Bank} operations
     */
    Person<?> getPersonByPassport(String passport, Localization type) throws RemoteException;

    /**
     * Creates new person with given parameters
     * <p>
     * If person with given passport already exists return existing person
     *
     * @param passport   person's passport
     * @param name       person's name
     * @param familyName person's second name
     * @return {@link info.kgeorgiy.ja.churakova.bank.person.RemotePerson}
     * @throws RemoteException
     */
    Person<MyRemote> createPerson(String passport, String name, String familyName) throws RemoteException;

    /**
     * Get all accounts that owned by given person
     *
     * @param person person whom accounts need to get
     * @return all person {@link info.kgeorgiy.ja.churakova.bank.account.PersonsAccount}
     * @throws RemoteException
     */
    Set<Account> getPersonAccounts(Person<?> person) throws RemoteException;
}
