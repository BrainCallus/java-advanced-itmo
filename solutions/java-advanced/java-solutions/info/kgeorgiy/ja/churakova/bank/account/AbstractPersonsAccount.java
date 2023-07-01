package info.kgeorgiy.ja.churakova.bank.account;

import info.kgeorgiy.ja.churakova.bank.exceptions.WrongArgumentsException;
import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Abstract class implements {@link PersonsAccount}
 *
 * @param <T> defines localization
 */
public class AbstractPersonsAccount<T> extends RemoteAccount implements PersonsAccount<T>, Serializable {

    private final String passport;
    private final String selfId;

    /**
     * Initialize account with given id
     *
     * @param id account id. Expect format "person_passport:account_id"
     */
    public AbstractPersonsAccount(final String id) {
        super(id);
        String[] parts = id.split(":");
        checkId(parts);
        passport = parts[0];
        selfId = parts[1];
    }

    private void checkId(String[] parts) {
        if (parts.length != 2) {
            throw new WrongArgumentsException("Id for person's account must mutch pattern \"<passport>:<id>\"");
        }
    }

    /**
     * @return passport of {@link Person} owns this account
     * @throws RemoteException if occur
     */
    @Override
    public synchronized String getPersonsPassport() throws RemoteException {
        return passport;
    }

    /**
     * @return account id of this account
     * @throws RemoteException if occur
     */
    @Override
    public synchronized String getSelfId() throws RemoteException {
        return selfId;
    }
}
