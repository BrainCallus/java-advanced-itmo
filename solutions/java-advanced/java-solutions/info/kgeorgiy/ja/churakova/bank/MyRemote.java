package info.kgeorgiy.ja.churakova.bank;

import java.rmi.RemoteException;

/**
 * For remote instances
 */
@FunctionalInterface
public interface MyRemote {
    /**
     * Instead of {@link Object#toString()} for remote instances
     *
     * @return string representation of an object
     * @throws RemoteException if occur during getting parameters from instance that implements {@link java.rmi.Remote}
     */
    String stringView() throws RemoteException;
}
