package ca.gradfinder.algorithms;

import java.util.*;

/** Prefix tree for word completion; results are ordered by word frequency. */
public final class Trie {
    private static final class Node {
        final Map<Character, Node> children = new TreeMap<>();
        int frequency;
    }
    private final Node root = new Node();

    public void insert(String word, int frequency) {
        Node node = root;
        for (char c : word.toCharArray()) node = node.children.computeIfAbsent(c, k -> new Node());
        node.frequency = frequency;
    }

    public List<String> complete(String input, int limit) {
        String prefix = input.toLowerCase(Locale.ROOT);
        Node node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return List.of();
        }
        List<Map.Entry<String, Integer>> found = new ArrayList<>();
        collect(node, new StringBuilder(prefix), found);
        found.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        return found.stream().limit(limit).map(Map.Entry::getKey).toList();
    }

    private void collect(Node node, StringBuilder word, List<Map.Entry<String, Integer>> out) {
        if (node.frequency > 0) out.add(Map.entry(word.toString(), node.frequency));
        node.children.forEach((c, child) -> {
            word.append(c);
            collect(child, word, out);
            word.deleteCharAt(word.length() - 1);
        });
    }
}
