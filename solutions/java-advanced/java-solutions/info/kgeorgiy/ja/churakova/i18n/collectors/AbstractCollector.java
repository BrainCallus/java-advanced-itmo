package info.kgeorgiy.ja.churakova.i18n.collectors;

import info.kgeorgiy.ja.churakova.i18n.StatType;
import info.kgeorgiy.ja.churakova.i18n.Statistic;
import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.text.BreakIterator;
import java.util.*;

public abstract class AbstractCollector<T extends Comparable<T>> implements StatCollector<T> {
    protected Locale locale;
    protected BreakIterator iterator;
    protected final StatType statType;
    private static final List<String> replaced = List.of("\n", "\t", "    ", "  ", ";", "!", "\"", "?");

    protected AbstractCollector() {
        this.locale = Locale.getDefault();
        this.statType = getStatType();
    }

    protected AbstractCollector(final Locale locale) {
        this.locale = locale;
        statType = getStatType();
    }

    protected abstract T parse(String input, String cur_word, int pos) throws UnexpectedFormatException;

    protected abstract int compare(T first, T second);

    protected abstract void updateExternal(T newElement, Statistic<T> statistic);

    protected abstract void initBreakIterator(String text);

    @Override
    public Statistic<T> collectStatistics(Locale inputLoc, String input) {
        if (input.isEmpty() || input.isBlank()) {
            return new Statistic<>(this.statType);
        }
        Statistic<T> statistic = new Statistic<>(this.statType);
        setOrCheckLocale(inputLoc);
        List<T> entryList = new ArrayList<>();
        initBreakIterator(input);
        int cur = iterator.first();
        for (int iter = iterator.next(); iter != BreakIterator.DONE; cur = iter, iter = iterator.next()) {
            String cur_word = input.substring(cur, iter);

            T parsed = parse(input, cutIgnoredCharacters(cur_word), cur);
            if (parsed != null) {
                updateStat(parsed, statistic);
                entryList.add(parsed);
            }
        }
        statistic.setEntries(entryList.size());
        statistic.setUnique(entryList.stream().distinct().count());
        statistic.setAvg(statistic.getAvg() / statistic.getEntries());

        return statistic;
    }

    private void setOrCheckLocale(Locale newLocale) {
        if (newLocale == null) {
            if (this.locale == null) {
                throw new UnexpectedFormatException("Can't get statistic without Locale");
            }
            return;
        }
        this.locale = newLocale;
    }

    private void updateStat(T newElement, Statistic<T> statistic) {
        if (statistic.getMin() == null) {
            statistic.setMin(newElement);
            statistic.setMax(newElement);
        } else {
            statistic.setMin(firstIsLess(newElement, statistic.getMin()) ? newElement : statistic.getMin());
            statistic.setMax(firstIsLess(statistic.getMax(), newElement) ? newElement : statistic.getMax());
        }

        updateExternal(newElement, statistic);
    }

    private boolean firstIsLess(T first, T second) {
        return compare(first, second) < 0;
    }

    private String cutIgnoredCharacters(String cur_word) {
        return replaced.stream().reduce(cur_word.trim(), (word, sym) -> word.replace(sym, ""));
    }

    private StatType getStatType() {
        return StatType.valueOf(this.getClass().getSimpleName().split("Collector")[0].toUpperCase());
    }
}
