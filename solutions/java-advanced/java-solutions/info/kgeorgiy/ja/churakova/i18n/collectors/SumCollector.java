package info.kgeorgiy.ja.churakova.i18n.collectors;

import info.kgeorgiy.ja.churakova.i18n.Statistic;
import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.text.BreakIterator;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Objects;

public class SumCollector extends AbstractCollector<Double> {
    public SumCollector() {
        super();
    }

    public SumCollector(Locale locale) {
        super(locale);
    }

    protected int compare(Double first, Double second) {
        return first.compareTo(second);
    }

    @Override
    protected void updateExternal(Double newElement, Statistic<Double> statistic) {
        statistic.setAvg(statistic.getAvg() + newElement);
        statistic.setMinLen(-1);
    }

    @Override
    protected void initBreakIterator(String text) {
        iterator = BreakIterator.getWordInstance(locale);
        iterator.setText(text);
    }

    @Override
    protected Double parse(String input, String cur_word, int pos) throws UnexpectedFormatException {
        ParsePosition position = new ParsePosition(pos);
        Number res = NumberFormat.getCurrencyInstance(locale).parse(input, position);
        try {
            return Objects.requireNonNull(res).doubleValue();
        } catch (NullPointerException nul) {
            return null;
        }
    }
}
