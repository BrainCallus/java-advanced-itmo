package info.kgeorgiy.ja.churakova.bank.tests;

import info.kgeorgiy.ja.churakova.bank.Local;
import info.kgeorgiy.ja.churakova.bank.Localization;
import info.kgeorgiy.ja.churakova.bank.MyRemote;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.RemotePersonAccount;
import info.kgeorgiy.ja.churakova.bank.bank.AccountOperations;
import info.kgeorgiy.ja.churakova.bank.bank.Bank;
import info.kgeorgiy.ja.churakova.bank.bank.RemoteBank;
import info.kgeorgiy.ja.churakova.bank.person.Person;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static info.kgeorgiy.ja.churakova.bank.tests.Utilits.*;

/**
 * Tests operations for {@link Bank} and {@link Person} interfaces
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTest {
    private static Bank bank;

    /**
     * Starts server for tests
     */
    @BeforeClass
    public static void startServer() {
        try {
            final Registry registry = LocateRegistry.createRegistry(DEFAULT_PORT);
            final Bank bank = new RemoteBank(DEFAULT_PORT);
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            registry.rebind(DEFAULT_HOST, bank);
            BankTest.bank = (Bank) LocateRegistry.getRegistry(DEFAULT_PORT).lookup(DEFAULT_HOST);
            System.out.println("Bank tests started");
        } catch (RemoteException | NotBoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Finalize tests. Realise host
     */
    @AfterClass
    public static void closeServer() {
        try {
            LocateRegistry.getRegistry(DEFAULT_PORT).unbind(DEFAULT_HOST);
        } catch (RemoteException | NotBoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests single person creation and subsequent search in bank base
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test1_singlePerson() throws RemoteException {
        Person<MyRemote> person = (Person<MyRemote>) bank.getPersonByPassport("0", Localization.REMOTE);
        assertNull(person, "person");
        final Person<MyRemote> person1 = bank.createPerson("0", "single", "Ivanov");
        assertNotNull(person1, "person");
        final Person<MyRemote> foundedPerson = (Person<MyRemote>) bank.getPersonByPassport("0", Localization.REMOTE);
        assertNotNull(foundedPerson, "person");
    }

    /**
     * Tests several person creation and subsequent search in bank base
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test2_severalPersons() throws RemoteException {
        for (int i = 1; i < 5; i++) {
            bank.createPerson(Integer.toString(i * 10), "person" + i * 10, "Family" + i * 10);
        }
        for (int i = 1; i < 5; i++) {
            final Person<MyRemote> expectNotNull =
                    (Person<MyRemote>) bank.getPersonByPassport(Integer.toString(i * 10), Localization.REMOTE);
            final Person<Local<RemotePersonAccount>> expectNotNullLoc =
                    (Person<Local<RemotePersonAccount>>) bank.getPersonByPassport(Integer.toString(i * 10), Localization.LOCAL);
            assertNotNull(expectNotNull, "person");
            assertNotNull(expectNotNullLoc, "person");
            Assert.assertEquals("Expected that local and remote persons are the same", expectNotNullLoc, expectNotNull);
            final Person<?> expectNull = bank.getPersonByPassport(Integer.toString(i), Localization.REMOTE);
            assertNull(expectNull, "person");
        }
    }

    /**
     * Tests operations with {@link RemotePersonAccount}
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public void test3_personsAccount() throws RemoteException {
        List<Person<?>> persons = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            persons.add(bank.getPersonByPassport(Integer.toString(i * 10), Localization.REMOTE));
        }
        for (Person<?> person : persons) {
            String prefix = person.getPassport() + ":";
            assertNotNull(person, "person");
            Assert.assertEquals("No accounts expected", 0, person.getAccounts().size());
            bank.createAccount(prefix + "100");
            assertNull(person.getAccountById("101"), "account");
            bank.createAccount(prefix + "101");
            assertNotNull(bank.getAccountById(prefix + "100"), "account");
            assertNotNull(bank.getAccountById(prefix + "101"), "account");
            String accountId = prefix + "100";
            person.getAccountById("100").setAmount(300);
            assertAmount(bank.getAccountById(accountId), 300);
        }
    }

    /**
     * Tests functional for {@link info.kgeorgiy.ja.churakova.bank.person.LocalPerson} and {@link info.kgeorgiy.ja.churakova.bank.account.LocalPersonAccount}
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test4_localPerson() throws RemoteException {
        final Person<Local<RemotePersonAccount>> personLoc =
                (Person<Local<RemotePersonAccount>>) bank.getPersonByPassport("10", Localization.LOCAL);
        assertNotNull(personLoc, "person");
        final Person<MyRemote> personRem = (Person<MyRemote>) bank.getPersonByPassport("10", Localization.REMOTE);
        personRem.getAccountById("101").setAmount(99);
        personRem.createAccount("102");

        personRem.getAccountById("102").setAmount(200);
        personLoc.getAccountById("100").setAmount(1000);
        personLoc.createAccount("103");
        personLoc.getAccountById("103").setAmount(1000);
        assertNull(personLoc.getAccountById("102"), "account");
        assertNull(personRem.getAccountById("103"), "account");
        Assert.assertEquals("3 accounts expected for localPerson", 3, personLoc.getAccounts().size());
        Assert.assertEquals("3 accounts expected for remotePerson", 3, personRem.getAccounts().size());
        assertionForTest4("local", "100", 1000, personLoc);
        assertionForTest4("local", "101", 0, personLoc);
        assertionForTest4("remote", "100", 300, personRem);
        assertionForTest4("remote", "101", 99, personRem);
    }

    /**
     * Multi thread tests for {@link Bank} operations
     * <p>
     * Tests parallel account creation and changing accounts balance
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public void test5_multiThread() throws RemoteException {
        List<String> passports = List.of("1012", "227", "3", "99", "66666");
        List<String> accountIds = List.of("888", "9", "124");
        List<Person<MyRemote>> clients = passports.stream().map(
                p -> {
                    try {
                        return bank.createPerson(p, "name" + p, "family" + p);
                    } catch (RemoteException e) {
                        Assert.fail(e.getMessage());
                    }
                    return null;
                }
        ).toList();
        ExecutorService executors = Executors.newFixedThreadPool(10);
        Phaser phaser = new Phaser(1);

        for (var client : clients) {
            String prefix = client.getPassport() + ":";
            for (var id : accountIds) {
                addAccountTask(prefix + id, executors, phaser);
            }
        }
        phaser.arriveAndAwaitAdvance();

        for (var client : clients) {
            for (var id : accountIds) {
                assertNotNull(client.getAccountById(id), "account");
            }
        }

        List<Integer> amounts = List.of(100, 500, 2000, 5000, 1000);
        int sum = amounts.stream().reduce(0, Integer::sum);
        for (var client : clients) {
            String prefix = client.getPassport() + ":";
            for (var id : accountIds) {
                for (var val : amounts) {
                    addSumTask(client, id, 2 * val, executors, phaser);
                    addSumTask(bank, prefix + id, -val, executors, phaser);
                }
            }
        }
        phaser.arriveAndAwaitAdvance();

        for (var client : clients) {
            for (var id : accountIds) {
                assertAmount(client.getAccountById(id), sum);
            }
        }
        executors.shutdown();
    }

    private void addAccountTask(String arg, ExecutorService executors, Phaser phaser) {
        executors.submit(() -> {
            phaser.register();
            try {
                bank.createAccount(arg);
            } catch (RemoteException rem) {
                Assert.fail(rem.getMessage());
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    private void addSumTask(AccountOperations caller, String arg, int val, ExecutorService executors, Phaser phaser) {
        executors.submit(() -> {
            phaser.register();
            try {
                synchronized (bank) {
                    Account account = caller.getAccountById(arg);
                    account.setAmount(account.getAmount() + val);
                }
            } catch (RemoteException e) {
                Assert.fail(e.getMessage());
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    private void assertionForTest4(String type, String id, int expected, Person<?> person) throws RemoteException {
        Assert.assertEquals(String.format("For %s person on account %s %d expected", type, id, expected),
                expected, person.getAccountById(id).getAmount());
    }
}
