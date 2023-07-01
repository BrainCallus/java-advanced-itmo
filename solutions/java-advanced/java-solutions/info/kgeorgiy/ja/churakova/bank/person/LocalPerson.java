package info.kgeorgiy.ja.churakova.bank.person;

import info.kgeorgiy.ja.churakova.bank.Local;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.LocalPersonAccount;
import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;
import info.kgeorgiy.ja.churakova.bank.account.RemotePersonAccount;
import info.kgeorgiy.ja.churakova.bank.exceptions.BankException;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPerson extends AbstractPerson<Local<RemotePersonAccount>> implements Local<RemotePerson> {

    /**
     * Initialize local person with given passport, name and familyName
     *
     * @param passport   person's passport
     * @param name       person's name
     * @param familyName person's second name
     */
    public LocalPerson(String passport, String name, String familyName) {
        super(passport, name, familyName);
    }

    /**
     * Creates local copy of given existing {@link RemotePerson}
     *
     * @param remotePerson person to copy
     */
    public LocalPerson(RemotePerson remotePerson) {
        super(remotePerson.getPassport(), remotePerson.getName(), remotePerson.getFamilyName());
        copyDataFromSample(remotePerson);
    }


    @Override
    protected Account createAccountImpl(String accountId) throws RemoteException {
        if (accounts.containsKey(accountId)) {
            return accounts.get(accountId);
        }
        LocalPersonAccount account = new LocalPersonAccount(passport + ":" + accountId);
        accounts.put(account.getSelfId(), account);
        return account;
    }

    @Override
    protected ConcurrentHashMap<String, PersonsAccount<Local<RemotePersonAccount>>> initAccountMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Makes local copies from given {@link RemotePerson} accounts
     *
     * @param sample object from which makes copy
     */
    @Override
    public void copyDataFromSample(RemotePerson sample) {
        sample.accounts.forEach((id, remAccount) -> {
            try {
                LocalPersonAccount newAc = new LocalPersonAccount(remAccount);
                this.accounts.put(newAc.getSelfId(), newAc);
            } catch (RemoteException e) {
                throw new BankException(String.format("Remote exception occur during creation LocalAccount %s%n%s", id, e.getMessage()));
            }
        });
    }
}
