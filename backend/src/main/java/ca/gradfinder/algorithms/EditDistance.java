package ca.gradfinder.algorithms;

/** Levenshtein distance using dynamic programming. Time O(n*m), memory O(m). */
public final class EditDistance implements StringDistance {
    @Override public int between(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int replace = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(replace, Math.min(previous[j], current[j - 1]) + 1);
            }
            int[] swap = previous; previous = current; current = swap;
        }
        return previous[b.length()];
    }
}
