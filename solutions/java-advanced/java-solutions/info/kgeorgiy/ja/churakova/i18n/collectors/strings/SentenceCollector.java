package info.kgeorgiy.ja.churakova.i18n.collectors.strings;

import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.text.BreakIterator;
import java.util.Locale;

public class SentenceCollector extends AbstractStringCollector {
    public SentenceCollector() {
        super();
    }

    @SuppressWarnings("unused")
    public SentenceCollector(Locale locale) {
        super(locale);
    }

    @Override
    protected void initBreakIterator(String text) {
        iterator = BreakIterator.getSentenceInstance(locale);
        iterator.setText(text);
    }

    @Override
    protected String parse(String input, String cur_word, int pos) throws UnexpectedFormatException {
        return cur_word;
    }
}
