package info.kgeorgiy.ja.churakova.bank.bank;

import info.kgeorgiy.ja.churakova.bank.Localization;
import info.kgeorgiy.ja.churakova.bank.MyRemote;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;
import info.kgeorgiy.ja.churakova.bank.account.RemoteAccount;
import info.kgeorgiy.ja.churakova.bank.account.RemotePersonAccount;
import info.kgeorgiy.ja.churakova.bank.exceptions.BankException;
import info.kgeorgiy.ja.churakova.bank.person.LocalPerson;
import info.kgeorgiy.ja.churakova.bank.person.Person;
import info.kgeorgiy.ja.churakova.bank.person.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person<MyRemote>> persons = new ConcurrentHashMap<>();

    /**
     * Default constructor with port
     *
     * @param port port to export
     */
    public RemoteBank(final int port) {
        this.port = port;
    }

    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account.
     */
    @Override
    public Account createAccount(final String id) throws RemoteException {
        Account account;
        if (id.contains(":")) {
            account = createPersonAccount(id);
        } else {
            account = new RemoteAccount(id);
        }
        return exportAccount(account);
    }

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    @Override
    public synchronized Account getAccountById(final String id) {
        return accounts.get(id);
    }

    /**
     * Creates new person with given parameters
     * <p>
     * If person with given passport already exists return existing person
     *
     * @param passport   person's passport
     * @param name       person's name
     * @param familyName person's second name
     * @return {@link info.kgeorgiy.ja.churakova.bank.person.RemotePerson}
     * @throws RemoteException if occur
     */
    @Override
    public Person<MyRemote> createPerson(String passport, String name, String familyName) throws RemoteException {
        if (requireNotNull(passport, name, familyName)) {
            final RemotePerson person = new RemotePerson(passport, name, familyName, port);
            if (persons.putIfAbsent(passport, person) == null) {
                UnicastRemoteObject.exportObject(person, port);
                return person;
            } else {
                return getRemotePerson(passport);
            }

        }
        System.err.println("Can't create new person because some arguments is null");
        return null;
    }

    /**
     * Find person in bank by given passport
     *
     * @param passport person's passport
     * @param type     {@link Localization} remote or local
     * @return {@link Person} founded in {@link Bank} or null, if not found
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Override
    public Person<?> getPersonByPassport(String passport, Localization type) throws RemoteException {
        if (requireNotNull(passport)) {
            return type == Localization.REMOTE ? getRemotePerson(passport) : getLocalPerson(passport);
        }
        System.err.println("Can't find person because passport is null");
        return null;
    }

    /**
     * Get all accounts that owned by given person
     *
     * @param person person whom accounts need to get
     * @return all person {@link info.kgeorgiy.ja.churakova.bank.account.PersonsAccount}
     * @throws RemoteException if occur
     */
    @Override
    public Set<Account> getPersonAccounts(Person<?> person) throws RemoteException {
        if (requireNotNull(person)) {
            person.getAccounts();
        }
        System.err.println("Can't get person's accounts, because person is null");
        return null;
    }

    private void addAccountToPerson(String id, PersonsAccount<MyRemote> account) throws RemoteException {
        RemotePerson person = (RemotePerson) getPersonByPassport(id, Localization.REMOTE);
        if (person == null) {
            throw new BankException(String.format("No such person with passport %s. Can't create account", id));
        }
        person.addAccount(account);
    }

    private RemotePerson getRemotePerson(String passport) {
        return (RemotePerson) persons.get(passport);
    }

    private LocalPerson getLocalPerson(String passport) {
        RemotePerson remPerson = (RemotePerson) persons.get(passport);
        return remPerson == null ? null : new LocalPerson(remPerson);
    }

    private PersonsAccount<MyRemote> createPersonAccount(String id) throws RemoteException {
        PersonsAccount<MyRemote> account = new RemotePersonAccount(id);
        addAccountToPerson(id.split(":")[0], account);
        return account;
    }

    private Account exportAccount(Account account) throws RemoteException {
        if (accounts.putIfAbsent(account.getId(), account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccountById(account.getId());
        }
    }

    private boolean requireNotNull(Object... args) {
        for (Object arg : args) {
            if (arg == null) {
                return false;
            }
        }
        return true;
    }
}
