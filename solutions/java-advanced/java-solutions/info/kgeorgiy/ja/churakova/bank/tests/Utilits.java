package info.kgeorgiy.ja.churakova.bank.tests;

import info.kgeorgiy.ja.churakova.bank.account.Account;
import org.junit.Assert;

import java.rmi.RemoteException;

public class Utilits {
    protected static final String DEFAULT_HOST = "//localhost/bank";
    protected static final int DEFAULT_PORT = 8888;

    protected static String wrongAmountMessage(String id, int expectedSum) {
        return String.format("For account %s expected amount %d%n", id, expectedSum);
    }

    protected static void assertAmount(Account account, int expected) throws RemoteException {
        Assert.assertEquals(wrongAmountMessage(account.getId(), expected), expected, account.getAmount());
    }

    protected static void assertNotNull(Object object, String type) {
        Assert.assertNotNull(String.format("Not null %s expected%n", type), object);
    }

    protected static void assertNull(Object object, String type) {
        Assert.assertNull(String.format("Null %s expected%n", type), object);
    }
}
