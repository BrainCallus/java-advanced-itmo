package info.kgeorgiy.ja.churakova.bank.account;

import info.kgeorgiy.ja.churakova.bank.MyRemote;

import java.rmi.RemoteException;

public class RemotePersonAccount extends AbstractPersonsAccount<MyRemote> implements MyRemote {
    /**
     * Initialize account with given id
     *
     * @param id account id. Expect format "person_passport:account_id"
     */
    public RemotePersonAccount(String id) {
        super(id);
    }

    /**
     * Instead of {@link Object#toString()} for this class
     *
     * @return string representation of an account
     * @throws RemoteException if occur during getting parameters from instance that implements {@link java.rmi.Remote}
     */
    @Override
    public String stringView() throws RemoteException {
        return String.format("RemotePersonAccount%n Owner: %s%n AccountId: %s", getPersonsPassport(), getSelfId());
    }
}
