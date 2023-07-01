package info.kgeorgiy.ja.churakova.i18n.collectors;

import info.kgeorgiy.ja.churakova.i18n.Statistic;

import java.util.Locale;

public interface StatCollector<T extends Comparable<T>> {
    Statistic<T> collectStatistics(Locale inputLocale, String source);
}
