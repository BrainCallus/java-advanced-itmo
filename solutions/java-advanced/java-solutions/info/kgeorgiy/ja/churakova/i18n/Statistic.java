package info.kgeorgiy.ja.churakova.i18n;

import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import static info.kgeorgiy.ja.churakova.i18n.Utilits.getBundle;

public class Statistic<T extends Comparable<T>> {
    private final StatType statType;
    private long entries;
    private long unique;
    private T min;
    private T max;
    private double avg;
    private long minLen;
    private long maxLen;

    public Statistic(StatType statType) {
        this.statType = statType;
        this.entries = 0;
        this.unique = 0;
        this.avg = 0;
        this.minLen = 0;
        this.maxLen = 0;
    }

    public long getEntries() {
        return entries;
    }

    public long getUnique() {
        return unique;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public double getAvg() {
        return avg;
    }

    @SuppressWarnings("unchecked")
    public T getAvgT() {
        if (this.statType != StatType.DATE) {
            throw new UnexpectedFormatException(String.format("%s statistic not support Date%n", statType()));
        }
        return (T) new Date((long) avg);
    }

    public long getMinLen() {
        if (this.statType != StatType.WORD && this.statType != StatType.SENTENCE) {
            throw new UnexpectedFormatException(String.format("%s statistic not support Length %n", statType()));
        }
        return minLen;
    }

    public long getMaxLen() {
        if (this.statType != StatType.WORD && this.statType != StatType.SENTENCE) {
            throw new UnexpectedFormatException(String.format("%s statistic not support Length %n", statType()));
        }
        return maxLen;
    }

    public void setEntries(long entries) {
        this.entries = entries;
    }

    public void setMinLen(long minLen) {
        this.minLen = minLen;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public void setMaxLen(long maxLen) {
        this.maxLen = maxLen;
    }

    public void setMax(T max) {
        this.max = max;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public void setUnique(long unique) {
        this.unique = unique;
    }

    public String getReport(Locale repLocale) {
        ResourceBundle bundle = getBundle(repLocale);
        String report = String.format("%s %s%n\t%s %s: %d (%s %d).%n",
                bundle.getString("_STAT"), bundle.getString(getBundleString(statType(), "S")),
                bundle.getString("AMOUNT"), bundle.getString(getBundleString(statType(), "U")),
                entries, bundle.getString("UNIQUE"), unique);
        report += getMinMaxReport(repLocale, bundle, "MIN", min);
        report += getMinMaxReport(repLocale, bundle, "MAX", max);
        report += addExtendedStat(bundle);
        return report;
    }

    private String getBundleString(String... args) {
        return Arrays.stream(args).reduce("", String::concat);
    }

    private String addExtendedStat(ResourceBundle bundle) {
        String addition = String.format(
                "\t%s: %s%n",
                bundle.getString("AVG_" + statType),
                statType == StatType.DATE ?
                        valueToString(getAvgT(), bundle.getLocale()) :
                        Utilits.defaultFormatNUMBER(getAvg(), bundle.getLocale())
        );

        if (minLen > 0) {
            addition += String.format(
                    "\t%s: %s (\"%s\").%n\t%s: %s (\"%s\").%n",
                    bundle.getString("MIN_LEN_" + statType()), getMinLen(), getMin(),
                    bundle.getString("MAX_LEN_" + statType()), getMaxLen(), getMax()
            );
        }
        return addition;
    }

    private String valueToString(T val, Locale locale) {
        if (val == null) {
            return "-";
        }

        try {
            Method format = Utilits.class.getDeclaredMethod("defaultFormat" + statType, val.getClass(), locale.getClass());
            format.setAccessible(true);
            return (String) format.invoke(Utilits.class, val, locale);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            System.err.println(e.getMessage());
        }

        return "MALFORMED_DATA";
    }

    private String getMinMaxReport(Locale reportLoc, ResourceBundle bundle, String key, T val) {
        return String.format("\t%s : %s.%n",
                bundle.getString(getBundleString(key, "_", statType())), valueToString(val, reportLoc));
    }

    private String statType() {
        return statType.toString();
    }
}
