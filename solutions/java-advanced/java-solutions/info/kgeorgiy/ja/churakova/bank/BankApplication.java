package info.kgeorgiy.ja.churakova.bank;

import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.bank.Bank;
import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.rmi.RemoteException;

/**
 * BankApplication interface
 */
public interface BankApplication {
    /**
     * Changes balance on account for person with given parameters
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    void changeBalance(String name, String familyName, String passport, String accountId, int delta) throws RemoteException;

    /**
     * Check that person in balance changing request and founded by given passport in the bank are same
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param sample     person that in bank by passport found
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    void verifyPerson(String name, String familyName, String passport, Person<MyRemote> sample) throws RemoteException;

    /**
     * Increase account balance
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes. Must be not negative
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    void topUpAccount(String name, String familyName, String passport, String accountId, int delta) throws RemoteException;

    /**
     * Decrease account balance
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes. Must be not negative
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    void debitAccount(String name, String familyName, String passport, String accountId, int delta) throws RemoteException;
}
