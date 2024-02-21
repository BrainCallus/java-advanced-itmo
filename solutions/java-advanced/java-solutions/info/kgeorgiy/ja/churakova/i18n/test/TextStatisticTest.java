package info.kgeorgiy.ja.churakova.i18n.test;

import info.kgeorgiy.ja.churakova.i18n.Statistic;
import info.kgeorgiy.ja.churakova.i18n.TextStatistics;
import info.kgeorgiy.ja.churakova.i18n.collectors.AbstractCollector;
import info.kgeorgiy.ja.churakova.i18n.collectors.DateCollector;
import info.kgeorgiy.ja.churakova.i18n.collectors.NumberCollector;
import info.kgeorgiy.ja.churakova.i18n.collectors.SumCollector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

public class TextStatisticTest {
    private static String pathPrefix = "solutions/java-advanced/samples/";

    public static void main(String[] args) {
        JUnitCore jUnit = new JUnitCore();
        jUnit.addListener(new TextListener(System.out));
        System.exit(runTests(jUnit, TextStatisticTest.class) ? 0 : 1);
    }

    private static boolean runTests(JUnitCore jUnit, Class<?>... classes) {
        return jUnit.run(classes).wasSuccessful();
    }

    @Test
    public void test1_numbers() {
        application("simple_numbers.txt", "output_simple_numbers.txt", false);
    }

    @Test
    public void test2_numberText() {
        application("numbers.txt", "output_numbers.txt", false);
    }

    @Test
    public void test3_big() {
        application("big_text.txt", "output_big.txt", false);
        application("big_text.txt", "out_big_ru.txt", true);
    }

    @Test
    public void test4_numbers() {
        AbstractCollector<Double> collector = new NumberCollector(Locale.US);
        String numbers = "10 2 -123456 100 13,99 13.99 19$ 1,33,900 13.9900";
        Statistic<Double> statistic = collector.collectStatistics(Locale.US, numbers);
        Assert.assertEquals("Expected 10 numbers", 10, statistic.getEntries());
        Assert.assertEquals("Expected 9 unique", 9, statistic.getUnique());
        Assert.assertEquals("Max 133900 expected", Optional.of(133900.0), Optional.of(statistic.getMax()));
        Assert.assertEquals("Min 133900 expected", Optional.of(-123456.0), Optional.of(statistic.getMin()));
    }

    @Test
    public void test5_dates() {
        AbstractCollector<Date> collector = new DateCollector(Locale.US);
        String dates = "19.04.1327 , 13:1:2002 13:01:2002 30.05.2024 77.05.1999 13.11.1987 27.01.3035 June 6, 1867 19.4.1327";
        Statistic<Date> statistic = collector.collectStatistics(Locale.US, dates);
        Assert.assertEquals("1 dates expected ", 1, statistic.getEntries());
        dates = "June 6, 1987 Jan 17, 2005 April 19, 1327    January 17, 2005 Sep 14, 1834 September 14, 1834 September 26, 3021";
        statistic = collector.collectStatistics(Locale.US, dates);
        Assert.assertEquals("7 dates expected", 7, statistic.getEntries());
        Assert.assertEquals("5 unique expected", 5, statistic.getUnique());
        Assert.assertEquals("Max is 26.09.3021", new Date(3021 - 1900, Calendar.SEPTEMBER, 26), statistic.getMax());
        Assert.assertEquals("Max is 19.04", new Date(1327 - 1900, Calendar.APRIL, 19), statistic.getMin());

    }

    @Test
    public void test6_money() {
        AbstractCollector<Double> collector = new SumCollector(Locale.US);
        String sums = "166.5 1,2345.90 $32,00 $3000 100,00 ₽ 100₽   $1,234.5";
        Statistic<Double> statistic = collector.collectStatistics(Locale.US, sums);
        Assert.assertEquals("3 sums expected", 3, statistic.getEntries());
    }

    private void application(String inputFile, String outputFile, boolean rus) {
        TextStatistics.main(Locale.US.getDisplayName(), (rus ? Locale.getDefault() : Locale.US).getDisplayName(),
                getStringPath(pathPrefix + inputFile), getStringPath(pathPrefix + outputFile));
        try {
            String s = Files.readString(Path.of(pathPrefix + outputFile).toAbsolutePath(), StandardCharsets.UTF_8);
            System.out.println(s);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    private String getStringPath(String file) {
        return Path.of(file).toAbsolutePath().toString();
    }
}
