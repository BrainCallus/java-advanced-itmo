package info.kgeorgiy.ja.churakova.i18n;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class Utilits {
    protected static String defaultFormatNUMBER(Double number, Locale locale) {
        return number == null ? null : new DecimalFormat("###,##0.0##").format(number);
    }

    protected static String defaultFormatWORD(String word, Locale locale) {
        return word;
    }

    protected static String defaultFormatSENTENCE(String sentence, Locale locale) {
        return sentence;
    }

    protected static String defaultFormatDATE(Date date, Locale locale) {
        return date == null ? null : DateFormat.getDateInstance(DateFormat.DATE_FIELD, locale).format(date);
    }

    protected static String defaultFormatSUM(Double sum, Locale locale) {
        return sum == null ? null : NumberFormat.getCurrencyInstance(locale).format(sum);
    }

    public static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(
                TextStatistics.class.getPackageName() + ".bundles.resource_bundle_" + locale, locale);
    }
}
