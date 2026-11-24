package ca.gradfinder.algorithms;

import java.util.*;

/** Maps word -> (document id -> number of occurrences). */
public final class InvertedIndex {
    private final Map<String, Map<Integer, Integer>> postings = new HashMap<>();
    private final Tokenizer tokenizer;

    public InvertedIndex(Tokenizer tokenizer) { this.tokenizer = tokenizer; }

    public void add(int documentId, String text) {
        for (String word : tokenizer.words(text))
            postings.computeIfAbsent(word, k -> new HashMap<>()).merge(documentId, 1, Integer::sum);
    }

    public Map<Integer, Integer> postings(String word) {
        return Collections.unmodifiableMap(postings.getOrDefault(word.toLowerCase(Locale.ROOT), Map.of()));
    }

    /** Documents containing every word, with total occurrences. The rarest word is processed first. */
    public Map<Integer, Integer> findAll(Collection<String> words) {
        if (words.isEmpty()) return Map.of();
        List<Map<Integer, Integer>> lists = words.stream().distinct().map(this::postings)
                .sorted(Comparator.comparingInt(Map::size)).toList();
        Map<Integer, Integer> result = new HashMap<>(lists.getFirst());
        for (Map<Integer, Integer> next : lists.subList(1, lists.size())) {
            result.keySet().retainAll(next.keySet());
            result.replaceAll((id, count) -> count + next.get(id));
        }
        return result;
    }

    public Map<String, Integer> vocabulary() {
        Map<String, Integer> out = new HashMap<>();
        postings.forEach((word, docs) -> out.put(word, docs.values().stream().mapToInt(Integer::intValue).sum()));
        return out;
    }
}
