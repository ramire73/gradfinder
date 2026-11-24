package ca.gradfinder.service;

import ca.gradfinder.algorithms.*;
import ca.gradfinder.model.ProgramRecord;
import ca.gradfinder.processing.ProgramDataset;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Combines the index, ranking, spell checking and completion over all loaded programs. */
@Service
public class SearchEngine {
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "of", "in", "and", "or", "for", "to", "with", "at", "on");
    private static final List<String> FILTERS = List.of("faculty", "degreeLevel", "programType", "deliveryMode");

    private record State(List<ProgramRecord> docs, Map<String, ProgramRecord> byUrl, InvertedIndex index,
                         SpellChecker spelling, Trie trie, int vocabularySize) {}
    public record Hit(ProgramRecord program, int score) {}
    public record Result(int total, List<Hit> hits, Map<String, List<String>> suggestions) {}

    private final ProgramDataset dataset;
    private final SearchHistory history;
    private final PatternFinder patternFinder;
    private final Tokenizer tokenizer = new Tokenizer();
    private final PageRanker ranker = new PageRanker();
    private final WordCounter counter = new WordCounter();
    private volatile State state; // replaced in one step, so searches never see a half-built index

    public SearchEngine(ProgramDataset dataset, SearchHistory history, PatternFinder patternFinder) {
        this.dataset = dataset;
        this.history = history;
        this.patternFinder = patternFinder;
    }

    @PostConstruct
    public void reload() { build(dataset.reload()); }

    void build(List<ProgramRecord> docs) {
        InvertedIndex index = new InvertedIndex(tokenizer);
        Map<String, ProgramRecord> byUrl = new HashMap<>();
        for (int i = 0; i < docs.size(); i++) {
            index.add(i, docs.get(i).text());
            byUrl.put(docs.get(i).url(), docs.get(i));
        }
        Map<String, Integer> vocabulary = index.vocabulary();
        Trie trie = new Trie();
        vocabulary.forEach(trie::insert);
        state = new State(docs, byUrl, index, new SpellChecker(vocabulary, new EditDistance()), trie, vocabulary.size());
    }

    public Result search(String query, Map<String, String> filters, int page, int size, boolean record) {
        State s = state;
        List<String> words = new ArrayList<>(new LinkedHashSet<>(tokenizer.words(query)));
        List<String> important = words.stream().filter(w -> !STOP_WORDS.contains(w)).toList();
        if (!important.isEmpty()) words = important;

        Map<Integer, Integer> scores;
        if (words.isEmpty()) { // browse everything
            scores = new HashMap<>();
            for (int i = 0; i < s.docs.size(); i++) scores.put(i, 0);
        } else {
            scores = new HashMap<>(s.index.findAll(words));
            if (record) { // unknown words are usually typing mistakes
                List<String> known = words.stream().filter(s.spelling::isKnown).toList();
                if (!known.isEmpty()) history.record(known);
            }
        }
        Map<String, List<String>> suggestions = new LinkedHashMap<>();
        if (scores.isEmpty())
            for (String w : words) if (!s.spelling.isKnown(w)) suggestions.put(w, s.spelling.suggest(w, 5));

        scores.keySet().removeIf(id -> !matches(s.docs.get(id), filters));
        var ranked = ranker.top(scores, (page + 1) * size);
        var hits = ranked.stream().skip((long) page * size)
                .map(r -> new Hit(withoutText(s.docs.get(r.documentId())), r.score())).toList();
        return new Result(scores.size(), hits, suggestions);
    }

    private static boolean matches(ProgramRecord p, Map<String, String> filters) {
        return filters.entrySet().stream().allMatch(f -> f.getValue() == null || f.getValue().isBlank()
                || filterValues(p, f.getKey()).contains(f.getValue()));
    }

    /** One program can have several values; the dataset separates them with "; ". */
    private static List<String> filterValues(ProgramRecord p, String key) {
        String value = key.equals("university") ? p.university() : p.fields().get(key);
        if (value == null) return List.of();
        return switch (key) {
            case "degreeLevel", "programType", "deliveryMode" -> List.of(value.split("; "));
            default -> List.of(value);
        };
    }

    private static ProgramRecord withoutText(ProgramRecord p) {
        return new ProgramRecord(p.university(), p.title(), p.url(), p.crawledAt(), p.fields(), "");
    }

    public Map<String, Set<String>> filterOptions() {
        Map<String, Set<String>> out = new LinkedHashMap<>();
        out.put("university", state.docs.stream().map(ProgramRecord::university).collect(Collectors.toCollection(TreeSet::new)));
        for (String f : FILTERS)
            out.put(f, state.docs.stream().flatMap(p -> filterValues(p, f).stream()).collect(Collectors.toCollection(TreeSet::new)));
        return out;
    }

    public List<String> complete(String prefix) { return state.trie.complete(prefix, 8); }

    public Map<String, Object> spell(String word) {
        boolean known = state.spelling.isKnown(word);
        return Map.of("word", word, "correct", known, "suggestions", known ? List.of() : state.spelling.suggest(word, 5));
    }

    public Map<String, Object> frequency(String word, String url) {
        return Map.of("word", word, "url", url, "count", counter.count(page(url).text(), word));
    }

    /** Where a word occurs according to the inverted index. */
    public List<Map<String, Object>> indexLookup(String word) {
        State s = state;
        return ranker.top(s.index.postings(word), 50).stream().map(r -> Map.<String, Object>of(
                "title", s.docs.get(r.documentId()).title(), "url", s.docs.get(r.documentId()).url(), "count", r.score())).toList();
    }

    public Map<String, Integer> patterns(String type, String url) {
        if (url != null && !url.isBlank()) return patternFinder.find(type, page(url).text());
        Map<String, Integer> out = new TreeMap<>();
        for (ProgramRecord p : state.docs) patternFinder.find(type, p.text()).forEach((k, v) -> out.merge(k, v, Integer::sum));
        return out;
    }

    /** Only pages that were already crawled can be used, so the server never opens a URL typed by a user. */
    private ProgramRecord page(String url) {
        ProgramRecord p = state.byUrl.get(url);
        if (p == null) throw new NoSuchElementException("That URL has not been crawled.");
        return p;
    }

    public List<Map<String, String>> pages() {
        return state.docs.stream().map(p -> Map.of("title", p.title(), "url", p.url())).toList();
    }

    public Map<String, Integer> stats() { return Map.of("programs", state.docs.size(), "vocabulary", state.vocabularySize); }
}
