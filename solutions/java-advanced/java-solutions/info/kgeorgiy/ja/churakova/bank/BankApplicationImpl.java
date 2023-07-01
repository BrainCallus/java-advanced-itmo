package info.kgeorgiy.ja.churakova.bank;

import info.kgeorgiy.ja.churakova.bank.account.Account;
import info.kgeorgiy.ja.churakova.bank.bank.Bank;
import info.kgeorgiy.ja.churakova.bank.bank.RemoteBank;
import info.kgeorgiy.ja.churakova.bank.exceptions.BankException;
import info.kgeorgiy.ja.churakova.bank.person.Person;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class BankApplicationImpl implements BankApplication {

    private static final String DEFAULT_HOST = "//localhost/bank";
    private static final int DEFAULT_PORT = 8888;

    private static Bank bank;

    /**
     * Default constructor
     *
     * @throws RemoteException       if occur
     * @throws MalformedURLException if occur
     * @throws NotBoundException     if occur
     */
    public BankApplicationImpl() throws RemoteException, MalformedURLException, NotBoundException {
        final Registry registry = LocateRegistry.createRegistry(DEFAULT_PORT + 1);
        final Bank bank = new RemoteBank(DEFAULT_PORT);
        UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
        registry.rebind(DEFAULT_HOST, bank);
        BankApplicationImpl.bank = (Bank) LocateRegistry.getRegistry(DEFAULT_PORT + 1).lookup(DEFAULT_HOST);
    }

    /**
     * Initialize bank with given host
     *
     * @param name host
     * @throws MalformedURLException if occur
     * @throws NotBoundException     if occur
     * @throws RemoteException       if occur
     */
    public BankApplicationImpl(String name) throws MalformedURLException, NotBoundException, RemoteException {
        bank = (Bank) LocateRegistry.getRegistry(DEFAULT_PORT).lookup(name);
    }


    /**
     * Changes balance on account for person with given parameters
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    @Override
    public void changeBalance(String name, String familyName, String passport, String accountId, int delta) throws RemoteException {

        Person<MyRemote> person = getAndVerifyPerson(passport, name, familyName);
        changeBalanceOrThrow(getPersonsAccount(person, accountId), delta);

    }

    /**
     * Check that person in balance changing request and founded by given passport in the bank are same
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param sample     person that in bank by passport found
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    @Override
    public void verifyPerson(String name, String familyName, String passport, Person<MyRemote> sample) throws RemoteException {
        if (!(sample.getName().equals(name) && sample.getFamilyName().equals(familyName) && sample.getPassport().equals(passport))) {
            throw new BankException("Attempt to illegal access detected: given person's information different to existing person. Person verification failed");
        }
    }

    /**
     * Increase account balance
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes. Must be not negative
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    @Override
    public void topUpAccount(String name, String familyName, String passport, String accountId, int delta) throws RemoteException {
        upOrDebit(name, familyName, passport, accountId, delta, true);
    }

    /**
     * Decrease account balance
     *
     * @param name       person's name
     * @param familyName person's second name
     * @param passport   person's passport
     * @param accountId  account to change balance
     * @param delta      amount by which the balance changes. Must be not negative
     * @throws RemoteException if occur in {@link Bank}, {@link Person} or {@link Account} operations
     */
    @Override
    public void debitAccount(String name, String familyName, String passport, String accountId, int delta) throws RemoteException {
        upOrDebit(name, familyName, passport, accountId, delta, false);
    }

    private void upOrDebit(String name, String familyName, String passport, String accountId, int delta, boolean increase) throws RemoteException {
        if (delta < 0) {
            throw new BankException("Unable to " + (increase ? "top up on" : "debit") + " negative sum " + delta);
        }
        changeBalance(name, familyName, passport, accountId, (increase ? delta : -delta));
    }

    private void changeBalanceOrThrow(Account account, int delta) throws RemoteException {
        if (account.getAmount() + delta < 0) {
            throw new BankException("Operation denied. To few money on balance");
        }
        account.setAmount(account.getAmount() + delta);
        System.out.printf("Balance on account %s successfully updated. Current amount: %d%n",
                account.getId(), account.getAmount());

    }

    private Account getPersonsAccount(Person<MyRemote> person, String accountId) throws RemoteException {
        Account account = person.getAccountById(accountId);
        if (account == null) {
            account = bank.createAccount(person.getPassport() + ":" + accountId);
        }
        return account;
    }

    private Person<MyRemote> getAndVerifyPerson(String passport, String name, String familyName) throws RemoteException {
        Person<MyRemote> person = (Person<MyRemote>) bank.getPersonByPassport(passport, Localization.REMOTE);
        if (person == null) {
            person = bank.createPerson(passport, name, familyName);
        }
        verifyPerson(name, familyName, passport, person);
        return person;
    }

    private static Stream<Method> getThisDeclaredPublicMethods() {
        return Arrays.stream(BankApplicationImpl.class.getDeclaredMethods()).
                filter(m -> Modifier.isPublic(m.getModifiers()));
    }

    private static boolean mayBeReflect(String name) {
        return getThisDeclaredPublicMethods().map(Method::getName).toList().contains(name);
    }

    private Method getByName(String name) {
        return getThisDeclaredPublicMethods().filter(m -> Objects.equals(m.getName(), name))
                .reduce(null, (m1, m2) -> m2);
    }

    private void executeByName(BankApplicationImpl that, String name, String familyName, String passport, String accountId, int delta, String methodName) {
        try {
            //Method method = that.getClass().getDeclaredMethod(methodName,String.class,String.class, String.class,String.class,int.class);
            // that the same to commented part but more simple
            Method method = getByName(methodName);
            Objects.requireNonNull(method).setAccessible(true);
            method
                    .invoke(that, name, familyName, passport, accountId, delta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void verifyArgs(String... args) {
        if (args == null || !(args.length >= 5 && args.length <= 7)) {
            throw new BankException("Args must not be null and their amount from 5 to 6 expected");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new BankException("Not null args expected");
            }
        }
    }

    /**
     * An entry point to application
     *
     * @param args arguments for execution
     */
    public static void main(String... args) {
        verifyArgs(args);
        BankApplicationImpl bankApplication;
        try {
            switch (args.length) {
                case 5 -> {
                    bankApplication = new BankApplicationImpl();
                    bankApplication.changeBalance(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
                }
                case 6 -> {
                    if (mayBeReflect(args[5])) {
                        bankApplication = new BankApplicationImpl();
                        bankApplication.executeByName(bankApplication, args[0], args[1], args[2], args[3], Integer.parseInt(args[4]), args[5]);
                    } else {
                        bankApplication = new BankApplicationImpl(args[0]);
                        bankApplication.changeBalance(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
                    }
                }
                case 7 -> {
                    bankApplication = new BankApplicationImpl(args[0]);
                    bankApplication.executeByName(bankApplication, args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), args[6]);
                }
            }
        } catch (RemoteException rem) {
            System.err.printf("Can't export bank %n%s%n", rem.getMessage());

        } catch (MalformedURLException mUrl) {
            System.err.printf(String.format("Can't initialize bank, because url is malformed%n%s%n", mUrl.getMessage()));

        } catch (NotBoundException nb) {
            System.err.printf(String.format("Bank not bound%n%s%n", nb.getMessage()));

        } catch (NumberFormatException num) {
            System.err.printf("Can't parse last argument %n%s%n", num.getMessage());
        }

    }

}
