package ca.gradfinder.algorithms;

import java.util.*;

/** Suggests vocabulary words that are close to a misspelled word. */
public final class SpellChecker {
    private final Map<String, Integer> vocabulary;
    private final StringDistance distances;

    public SpellChecker(Map<String, Integer> vocabulary, StringDistance distances) {
        this.vocabulary = vocabulary;
        this.distances = distances;
    }

    public boolean isKnown(String word) { return vocabulary.containsKey(word.toLowerCase(Locale.ROOT)); }

    public List<String> suggest(String input, int limit) {
        String word = input.toLowerCase(Locale.ROOT);
        int max = word.length() <= 4 ? 1 : 2;
        record Candidate(String word, int distance, int frequency) {}
        List<Candidate> found = new ArrayList<>();
        vocabulary.forEach((v, frequency) -> {
            if (Math.abs(v.length() - word.length()) > max) return; // cannot be within max edits
            int distance = distances.between(word, v);
            if (distance <= max) found.add(new Candidate(v, distance, frequency));
        });
        found.sort(Comparator.comparingInt(Candidate::distance)
                .thenComparing(Comparator.comparingInt(Candidate::frequency).reversed())
                .thenComparing(Candidate::word));
        return found.stream().limit(limit).map(Candidate::word).toList();
    }
}
