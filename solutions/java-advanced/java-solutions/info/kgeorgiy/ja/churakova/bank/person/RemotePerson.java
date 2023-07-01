package info.kgeorgiy.ja.churakova.bank.person;

import info.kgeorgiy.ja.churakova.bank.MyRemote;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;
import info.kgeorgiy.ja.churakova.bank.account.RemotePersonAccount;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson<MyRemote> implements MyRemote {
    private final int port;

    /**
     * Initialize person with given passport, name and familyName
     *
     * @param passport   person's passport
     * @param name       person's name
     * @param familyName person's second name
     * @param port       port to export
     */
    public RemotePerson(String passport, String name, String familyName, int port) {
        super(passport, name, familyName);
        this.port = port;
    }


    @Override
    protected Account createAccountImpl(String accountId) throws RemoteException {
        final PersonsAccount<MyRemote> account = new RemotePersonAccount(passport + ":" + accountId);
        if (accounts.putIfAbsent(account.getSelfId(), account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return accounts.get(accountId);
        }
    }

    @Override
    protected ConcurrentHashMap<String, PersonsAccount<MyRemote>> initAccountMap() {
        return new ConcurrentHashMap<>();
    }


    /**
     * String representation of this remote person
     * @return string representation of this remote person
     */
    @Override
    public String stringView() {
        return String.join(" ", getName(), getFamilyName(), getPassport());
    }


}
