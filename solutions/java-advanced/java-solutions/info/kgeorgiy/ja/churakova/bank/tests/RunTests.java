package info.kgeorgiy.ja.churakova.bank.tests;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

/**
 * Starts tests for {@link info.kgeorgiy.ja.churakova.bank.bank.Bank} and {@link info.kgeorgiy.ja.churakova.bank.BankApplication}
 */
public class RunTests {
    /**
     * An entry point
     *
     * @param args arbitrary parameters, as a rule are null
     */
    public static void main(String[] args) {
        JUnitCore jUnit = new JUnitCore();
        jUnit.addListener(new TextListener(System.out));
        System.exit(runTests(jUnit, BankTest.class, ApplicationTest.class) ? 0 : 1);
    }

    private static boolean runTests(JUnitCore jUnit, Class<?>... classes) {
        return jUnit.run(classes).wasSuccessful();
    }
}
