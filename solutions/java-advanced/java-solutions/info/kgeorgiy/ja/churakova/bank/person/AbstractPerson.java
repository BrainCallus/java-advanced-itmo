package info.kgeorgiy.ja.churakova.bank.person;

import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of {@link Person}
 *
 * @param <AccType> account type that owns person
 */
public abstract class AbstractPerson<AccType> implements Person<AccType>, Serializable {
    protected final String passport;
    protected final String name;
    protected final String familyName;
    protected ConcurrentHashMap<String, PersonsAccount<AccType>> accounts;

    /**
     * Initialize person with given passport, name and familyName
     *
     * @param passport   person's passport
     * @param name       person's name
     * @param familyName person's second name
     */
    public AbstractPerson(String passport, String name, String familyName) {
        this.passport = passport;
        this.name = name;
        this.familyName = familyName;
        accounts = initAccountMap();
    }

    /**
     * Return person's passport
     *
     * @return person's passport
     */
    @Override
    public String getPassport() {
        return passport;
    }

    /**
     * Return person's name
     *
     * @return person's name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Return person's second name
     *
     * @return person's second name
     */
    @Override
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Return all person's accounts
     *
     * @return person's accounts
     */
    @Override
    public Set<PersonsAccount<AccType>> getAccounts() {
        return new HashSet<>(accounts.values());
    }


    /**
     * Add already existing account for this person
     *
     * @param account account to add
     * @throws RemoteException if occur
     */
    public void addAccount(PersonsAccount<AccType> account) throws RemoteException {
        accounts.putIfAbsent(account.getSelfId(), account);
    }

    /**
     * return person's account with given id
     *
     * @param accountId account id
     * @return account with given id or null if this person have not such
     */
    @Override
    public Account getAccountById(String accountId) {
        return accountId == null ? null : accounts.get(accountId);
    }

    /**
     * Creates account for person with given id
     * <p>
     * If account with given id already exists return existing
     *
     * @param accountId account id
     * @return account that was just created
     * @throws RemoteException if occur
     */
    @Override
    public Account createAccount(String accountId) throws RemoteException {
        return accountId == null ? null : createAccountImpl(accountId);
    }

    @Override
    public String toString() {
        return String.join(" ", getName(), getFamilyName(), getPassport());
    }

    /**
     * Overridden {@link Object#equals} ()} method
     *
     * @param other object to compare
     * @return whether given object equals to this person
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Person<?> person) {
            try {
                return this.name.equals(person.getName())
                        && this.familyName.equals(person.getFamilyName())
                        && this.passport.equals(person.getPassport());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    protected abstract Account createAccountImpl(String accountId) throws RemoteException;

    protected abstract ConcurrentHashMap<String, PersonsAccount<AccType>> initAccountMap();
}
