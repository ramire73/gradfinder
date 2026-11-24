package ca.gradfinder.algorithms;

import java.util.*;

/** Keeps the k highest scoring pages in a min-heap: O(n log k) instead of sorting everything. */
public final class PageRanker {
    public record Ranked(int documentId, int score) {}

    public List<Ranked> top(Map<Integer, Integer> scores, int k) {
        if (k <= 0) return List.of();
        // Worst page at the head. Equal scores: lower id is better, so order is always the same.
        Comparator<Ranked> worstFirst = Comparator.comparingInt(Ranked::score)
                .thenComparing(Comparator.comparingInt(Ranked::documentId).reversed());
        PriorityQueue<Ranked> heap = new PriorityQueue<>(worstFirst);
        scores.forEach((id, score) -> {
            heap.add(new Ranked(id, score));
            if (heap.size() > k) heap.poll();
        });
        List<Ranked> out = new ArrayList<>(heap);
        out.sort(worstFirst.reversed());
        return out;
    }
}
