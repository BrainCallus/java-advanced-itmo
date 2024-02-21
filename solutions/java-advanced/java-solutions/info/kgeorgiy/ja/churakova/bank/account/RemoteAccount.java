package info.kgeorgiy.ja.churakova.bank.account;

import info.kgeorgiy.ja.churakova.bank.MyRemote;

import java.io.Serializable;
import java.rmi.RemoteException;

public class RemoteAccount implements Account, Serializable, MyRemote {
    protected final String id;
    private int amount;

    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }

    @Override
    public String stringView() throws RemoteException {
        return "RemoteAccount " + getId();
    }
}
