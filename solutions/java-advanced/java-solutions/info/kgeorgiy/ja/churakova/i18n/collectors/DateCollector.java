package info.kgeorgiy.ja.churakova.i18n.collectors;

import info.kgeorgiy.ja.churakova.i18n.Statistic;
import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class DateCollector extends AbstractCollector<Date> {

    public DateCollector() {
        super();
    }

    public DateCollector(Locale locale) {
        super(locale);
    }


    @Override
    protected int compare(Date first, Date second) {
        return first.compareTo(second);
    }

    @Override
    protected void updateExternal(Date newElement, Statistic<Date> statistic) {
        statistic.setAvg(statistic.getAvg() + newElement.getTime());
        statistic.setMinLen(-1);
    }

    @Override
    protected void initBreakIterator(String text) {
        iterator = BreakIterator.getWordInstance(locale);
        iterator.setText(text);
    }

    @Override
    protected Date parse(String input, String cur_word, int pos) throws UnexpectedFormatException {

        for (int format : new int[]{DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT}) {
            ParsePosition posit = new ParsePosition(pos);
            Date res = DateFormat.getDateInstance(format, locale).parse(input, posit);
            try {
                return Objects.requireNonNull(res);
            } catch (NullPointerException nul) {
                //
            }
        }
        return null;
    }
}
