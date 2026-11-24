package ca.gradfinder.algorithms;

import java.util.Locale;

/** Counts whole-word, case-insensitive occurrences with Knuth-Morris-Pratt, O(n + m). */
public final class WordCounter {
    public int count(String source, String target) {
        String text = source.toLowerCase(Locale.ROOT), word = target.toLowerCase(Locale.ROOT);
        if (word.isEmpty()) return 0;
        int[] failure = new int[word.length()];
        for (int i = 1, k = 0; i < word.length(); i++) {
            while (k > 0 && word.charAt(i) != word.charAt(k)) k = failure[k - 1];
            if (word.charAt(i) == word.charAt(k)) k++;
            failure[i] = k;
        }
        int count = 0;
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != word.charAt(k)) k = failure[k - 1];
            if (text.charAt(i) == word.charAt(k)) k++;
            if (k == word.length()) {
                int start = i - k + 1;
                boolean left = start == 0 || !Character.isLetterOrDigit(text.charAt(start - 1));
                boolean right = i + 1 == text.length() || !Character.isLetterOrDigit(text.charAt(i + 1));
                if (left && right) count++;
                k = failure[k - 1];
            }
        }
        return count;
    }
}
