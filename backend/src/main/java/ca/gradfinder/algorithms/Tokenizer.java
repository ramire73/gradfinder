package ca.gradfinder.algorithms;

import java.util.*;
import java.util.regex.*;

/** Splits text into lower-case words. */
public final class Tokenizer {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    public List<String> words(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }
}
