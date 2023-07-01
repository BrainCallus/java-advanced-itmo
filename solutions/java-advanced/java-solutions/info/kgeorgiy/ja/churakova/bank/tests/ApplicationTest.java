package info.kgeorgiy.ja.churakova.bank.tests;

import info.kgeorgiy.ja.churakova.bank.BankApplication;
import info.kgeorgiy.ja.churakova.bank.BankApplicationImpl;
import info.kgeorgiy.ja.churakova.bank.Localization;
import info.kgeorgiy.ja.churakova.bank.MyRemote;
import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.account.PersonsAccount;
import info.kgeorgiy.ja.churakova.bank.bank.Bank;
import info.kgeorgiy.ja.churakova.bank.bank.RemoteBank;
import info.kgeorgiy.ja.churakova.bank.exceptions.BankException;
import info.kgeorgiy.ja.churakova.bank.person.Person;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.function.BiPredicate;

import static info.kgeorgiy.ja.churakova.bank.tests.Utilits.*;

/**
 * Tests operations for {@link BankApplication}
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationTest {

    private static Bank bank;

    /**
     * Starts server for tests
     */
    @BeforeClass
    public static void startServer() {
        try {
            final Registry registry = LocateRegistry.getRegistry(DEFAULT_PORT);
            final Bank bank = new RemoteBank(DEFAULT_PORT);
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            registry.rebind(DEFAULT_HOST, bank);
            ApplicationTest.bank = (Bank) LocateRegistry.getRegistry(DEFAULT_PORT).lookup(DEFAULT_HOST);
            System.out.println("Application tests started");
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
     * Test application for already existing {@link Person}
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test1_applicationExistingPerson() throws RemoteException {
        simpleApplicationTest(getDefaultPersonByPassport("12345"));
    }

    /**
     * Tests application functional for persons that didn't exist before
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test2_applicationNoPerson() throws RemoteException {
        String name = "name678910";
        String family = "family678910";
        String passport = "678910";
        simpleApplicationTest(null, name, family, passport);
    }

    /**
     * Tests application for several persons
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test3_applicationSeveralPersons() throws RemoteException {
        List<Integer> passports = List.of(100, 200, 300, 400, 500, 600);
        for (int i = 0; i < passports.size(); i++) {
            if (i % 2 == 0) {
                simpleApplicationTest(getDefaultPersonByPassport(Integer.toString(passports.get(i))));
            } else {
                simpleApplicationTest(null,
                        getDefaultPersonParametersByPassport(Integer.toString(passports.get(i))).split(";"));
            }
        }
    }

    /**
     * Tests support of denying illegal access
     * <p>
     * Check {@link BankApplication#verifyPerson}
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test4_applicationIllegalAccess() throws RemoteException {
        for (int i = 1; i <= 6; i++) {
            Person<?> person = bank.getPersonByPassport(Integer.toString(i * 100), Localization.REMOTE);
            expectApplicationException(null, 100, "1", person.getName() + " ", person.getFamilyName(), person.getPassport());
            expectApplicationException(null, 100, "1", person.getName(), person.getFamilyName() + "!", person.getPassport());
        }
    }

    /**
     * Tests changing account balance on negative value. Tests that balance can't be negative
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test5_decreaseAmount() throws RemoteException {
        for (int i = 1; i <= 3; i++) {
            Person<MyRemote> person = (Person<MyRemote>) bank.getPersonByPassport(Integer.toString(i * 100), Localization.REMOTE);
            person.getAccounts().stream().filter(a -> AcIdPred.test(a, 5)).forEach(account -> {
                try {
                    applicationMainWithPerson(person, account.getSelfId(), 0);
                    int cur = account.getAmount();
                    applicationMainWithPerson(person, account.getSelfId(), -account.getAmount());
                    expectApplicationException(person, -1, account.getSelfId());

                    applicationMainWithPerson(person, account.getSelfId(), cur);
                } catch (RemoteException e) {
                    Assert.fail(e.getMessage());
                }
            });
        }
    }

    /**
     * Tests additional functional {@link BankApplication#debitAccount},{@link BankApplication#topUpAccount}
     *
     * @throws RemoteException if occur in {@link Bank} operations
     */
    @Test
    public final void test6_topUpDebit() throws RemoteException {
        for (int i = 4; i <= 6; i++) {
            Person<MyRemote> person = (Person<MyRemote>) bank.getPersonByPassport(Integer.toString(i * 100), Localization.REMOTE);
            person.getAccounts().stream().filter(a -> AcIdPred.test(a, 3)).forEach(account -> {
                try {
                    int cur_sum = account.getAmount();
                    applicationMainReflectMethod("topUpAccount", person, account.getSelfId(), 2 * cur_sum);
                    assertAmount(account, cur_sum * 3);
                    applicationMainReflectMethod("debitAccount", person, account.getSelfId(), cur_sum);
                    assertAmount(account, cur_sum * 2);
                } catch (RemoteException e) {
                    Assert.fail("Remote exception occur in BankApplication " + e.getMessage());
                }
            });
        }
    }

    /**
     * Tests {@link BankApplication} when don't given bank
     *
     * @throws RemoteException       if occur in {@link Bank} operations
     * @throws MalformedURLException if occur in {@link BankApplicationImpl} constructor
     * @throws NotBoundException     if occur in {@link BankApplicationImpl} constructor
     */
    @Test
    public final void test7_applicationNoBank() throws RemoteException, MalformedURLException, NotBoundException {

        try {
            BankApplicationImpl bankApplication = new BankApplicationImpl();
            Field fBank = bankApplication.getClass().getDeclaredField("bank");
            fBank.setAccessible(true);
            Bank newBank = (Bank) fBank.get(bank);
            for (int i = 1; i < 10; i++) {
                String[] personParams = getDefaultPersonParametersByPassport(Integer.toString(i * 1000)).split(";");
                for (int j = 0; j < 5; j++) {
                    String curAccount = Integer.toString(j * 10 + 1);
                    invokeReflectMethod(bankApplication, "changeBalance", personParams, curAccount, (i + j) * 10000);
                    invokeReflectMethod(bankApplication, "debitAccount", personParams, curAccount, j * 10000);
                }
                Person<MyRemote> curPerson = (Person<MyRemote>) newBank.getPersonByPassport(personParams[2], Localization.REMOTE);
                assertNotNull(curPerson, "person");
                for (int j = 0; j < 5; j++) {
                    String postfix = Integer.toString(j * 10 + 1);
                    Account curAccount = curPerson.getAccountById(postfix);
                    assertNotNull(curAccount, "person");
                    assertAmount(curAccount, i * 10000);
                    invokeReflectMethod(bankApplication, "topUpAccount", personParams, postfix, j * 10000);
                    assertAmount(curAccount, (i + j) * 10000);
                    exceptionExpectedReflect(bankApplication, "changeBalance", personParams, postfix, -2 * (i + j) * 10000);
                    exceptionExpectedReflect(bankApplication, "debitAccount", personParams, postfix, -1);
                    exceptionExpectedReflect(bankApplication, "topUpAccount", personParams, postfix, -1);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        }

    }


    private void exceptionExpectedReflect(BankApplication application, String methodName, String[] personParams, String accountId, int delta) {
        try {
            invokeReflectMethod(application, methodName, personParams, accountId, delta);
            Assert.fail("Illegal operation! BankException expected");
        } catch (BankException b) {
            Assert.assertNotNull(b);
        }
    }

    private void invokeReflectMethod(BankApplication application, String methodName, String[] personParams, String accountId, int delta) {
        try {
            Method method = application.getClass().getDeclaredMethod(methodName,
                    String.class, String.class, String.class, String.class, int.class);
            method.invoke(application, personParams[0], personParams[1], personParams[2], accountId, delta);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            Assert.fail(e.getMessage());
        } catch (InvocationTargetException b) {
            if (b.getTargetException() instanceof BankException) {
                throw new BankException(b.getTargetException().getMessage());
            } else {
                Assert.fail(b.getMessage());
            }

        }

    }

    private void expectApplicationException(Person<MyRemote> person, int delta, String... args) throws RemoteException {
        try {
            if (person == null) {
                applicationMainNoPerson(args[1], args[2], args[3], args[0], delta);
            } else {
                applicationMainWithPerson(person, args[0], delta);
            }
            Assert.fail("Illegal operation! BankException expected");
        } catch (BankException b) {
            Assert.assertNotNull(b);
        }
    }

    private void simpleApplicationTest(Person<MyRemote> person, String... args) throws RemoteException {
        for (int i = 0; i < 10; i++) {
            int expectedSum = 0;
            String id = Integer.toString(i);
            for (int j = 0; j < 20; j++) {
                expectedSum += j * i;
                if (person == null) {
                    applicationMainNoPerson(args[0], args[1], args[2], id, j * i);
                } else {
                    applicationMainWithPerson(person, id, j * i);
                }

            }
            assertAmount((person == null ? bank.getPersonByPassport(args[2], Localization.REMOTE) : person).getAccountById(id), expectedSum);
        }
    }

    private static void applicationMainReflectMethod(String methodName, Person<MyRemote> person, String accountId, int delta) throws RemoteException {
        BankApplicationImpl.main(DEFAULT_HOST, person.getName(), person.getFamilyName(),
                person.getPassport(), accountId, Integer.toString(delta), methodName);
    }

    private static void applicationMainWithPerson(Person<MyRemote> person, String accountId, int delta) throws RemoteException {
        BankApplicationImpl.main(DEFAULT_HOST, person.getName(), person.getFamilyName(), person.getPassport(),
                accountId, Integer.toString(delta));
    }

    private void applicationMainNoPerson(String name, String familyName, String passport, String accountId, int delta) throws RemoteException {
        BankApplicationImpl.main(DEFAULT_HOST, name, familyName, passport,
                accountId, Integer.toString(delta));
    }

    private static String getDefaultPersonParametersByPassport(String passport) {
        return String.join(";", "name" + passport, "family" + passport, passport);
    }

    private static Person<MyRemote> getDefaultPersonByPassport(String passport) throws RemoteException {
        return bank.createPerson(passport, "name" + passport, "family" + passport);
    }

    private final BiPredicate<PersonsAccount<?>, Integer> AcIdPred = (ac, n) -> {
        try {
            return Integer.parseInt(ac.getSelfId()) >= n;
        } catch (RemoteException e) {
            return false;
        }
    };

}
