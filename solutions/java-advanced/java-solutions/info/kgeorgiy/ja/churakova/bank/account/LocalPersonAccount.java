package info.kgeorgiy.ja.churakova.bank.account;

import info.kgeorgiy.ja.churakova.bank.Local;
import info.kgeorgiy.ja.churakova.bank.exceptions.WrongArgumentsException;
import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Implementation of {@link PersonsAccount} that not remote
 */
public class LocalPersonAccount extends AbstractPersonsAccount<Local<RemotePersonAccount>>
        implements Local<RemotePersonAccount>, Serializable {
    /**
     * Default constructor. Initialize account with id and 0 balance
     *
     * @param id account id
     */
    public LocalPersonAccount(String id) {
        super(id);
    }

    /**
     * Makes copy from {@link RemotePersonAccount}
     *
     * @param account {@link RemotePersonAccount} which this account copies
     * @throws RemoteException if Remote exception occur during {@link Account#getId()} or {@link Account#getAmount()}
     */
    public LocalPersonAccount(PersonsAccount<?> account) throws RemoteException {
        super(account.getId());
        checkInstance(account);
        copyDataFromSample((RemotePersonAccount) account);
    }

    /**
     * Creates LocalPersonAccount for given person
     *
     * @param person account owner
     * @param id     selfId for account
     * @throws RemoteException if RemoteExceptionOccur during {@link Person#getPassport()}
     */
    public LocalPersonAccount(Person<?> person, String id) throws RemoteException {
        super(person.getPassport() + ":" + id);
    }

    private void checkInstance(Account account) {
        if (!(account instanceof RemotePersonAccount)) {
            throw new WrongArgumentsException("Unable to create LocalPersonAccount from object that is not RemotePersonAccount");
        }
    }

    /**
     * Copies amount from initial {@link RemotePersonAccount}
     *
     * @param sample object from which makes copy
     */
    @Override
    public void copyDataFromSample(RemotePersonAccount sample) {
        sample.setAmount(sample.getAmount());
    }
}
