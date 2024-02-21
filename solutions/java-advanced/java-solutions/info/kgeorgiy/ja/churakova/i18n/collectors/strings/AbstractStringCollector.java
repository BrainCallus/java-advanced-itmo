package info.kgeorgiy.ja.churakova.i18n.collectors.strings;

import info.kgeorgiy.ja.churakova.i18n.Statistic;
import info.kgeorgiy.ja.churakova.i18n.collectors.AbstractCollector;

import java.text.Collator;
import java.util.Locale;
import java.util.Objects;

public abstract class AbstractStringCollector extends AbstractCollector<String> {
    private final Collator collator;

    protected AbstractStringCollector() {
        super();
        collator = Collator.getInstance(locale);
    }

    protected AbstractStringCollector(Locale locale) {
        super(locale);
        collator = Collator.getInstance(this.locale);
    }

    @Override
    protected int compare(String first, String second) {
        return first.length() - second.length() == 0 ? collator.compare(first, second) : first.length() - second.length();
    }

    @Override
    protected void updateExternal(String newElement, Statistic<String> statistic) {
        // if given string is correct it has at least 1 symbol
        statistic.setAvg(statistic.getAvg() + newElement.length());
        statistic.setMinLen(Objects.requireNonNullElse(statistic.getMin(), "").length());
        statistic.setMaxLen(Objects.requireNonNullElse(statistic.getMax(), "").length());
    }
}
