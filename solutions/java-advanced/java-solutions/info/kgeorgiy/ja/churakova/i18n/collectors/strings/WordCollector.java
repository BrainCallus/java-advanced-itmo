package info.kgeorgiy.ja.churakova.i18n.collectors.strings;

import info.kgeorgiy.ja.churakova.i18n.exceptions.UnexpectedFormatException;

import java.text.BreakIterator;
import java.util.function.Predicate;

public class WordCollector extends AbstractStringCollector {


    public WordCollector() {
        super();

    }


    @Override
    protected void initBreakIterator(String text) {
        iterator = BreakIterator.getWordInstance(locale);
        iterator.setText(text);
    }


    @Override
    protected String parse(String input, String cur_word, int pos) throws UnexpectedFormatException {
        if (!(cur_word.isBlank() || cur_word.isEmpty() || cur_word.equals("-")) &&
                cur_word.chars().allMatch(c -> isLetter.test(c))
        ) {
            return cur_word;
        }
        return null;
    }

    Predicate<Integer> isLetter = (c) -> c == '-' || Character.isLetter(c);

}
