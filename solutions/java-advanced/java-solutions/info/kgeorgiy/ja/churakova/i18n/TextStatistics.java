package info.kgeorgiy.ja.churakova.i18n;

import info.kgeorgiy.ja.churakova.bank.exceptions.WrongArgumentsException;

import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

public class TextStatistics {
    private static void verifyArgs(String[] args) {
        if (args == null || args.length != 4) {
            throw new WrongArgumentsException("Args must not be null and their amount 4 expected");
        }
        for (String arg : args) {
            if (arg == null) {
                throw new WrongArgumentsException("Not null args expected");
            }
        }
    }

    private static Locale getLocaleByName(String loc) throws NoSuchElementException {
        return Arrays.stream(Locale.getAvailableLocales())
                .filter(locale -> locale.getDisplayName().equals(loc)).findFirst().orElseThrow();
    }


    public static void main(String... args) {
        verifyArgs(args);

        try {
            Locale inputLocale = getLocaleByName(args[0]);
            Locale reportLocale = getLocaleByName(args[1]);
            String inputFile = args[2];
            String reportFile = args[3];
            StatisticOperator statOperator = new StatisticOperator();
            statOperator.collectStatistics(inputLocale, inputFile, StatType.FULL);
            statOperator.writeReport(inputFile, reportFile, reportLocale);
        } catch (NoSuchElementException ne) {
            System.err.printf("Can't get locale: %s%n", ne.getMessage());
        }

    }
}
