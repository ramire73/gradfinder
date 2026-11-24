package ca.gradfinder.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Search frequency: how many times each word was searched. Saved to disk between restarts. */
@Service
public class SearchHistory {
    private final Map<String, Integer> counts = new HashMap<>();
    private final ObjectMapper json;
    private final Path file;

    public SearchHistory(ObjectMapper json, @Value("${gradfinder.data-dir}") String dataDir) {
        this.json = json;
        this.file = Path.of(dataDir, "search-history.json");
        if (Files.exists(file)) counts.putAll(json.readValue(file.toFile(), new TypeReference<Map<String, Integer>>() {}));
    }

    public synchronized void record(Collection<String> words) {
        words.forEach(w -> counts.merge(w, 1, Integer::sum));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, json.writeValueAsString(counts));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public synchronized Map<String, Integer> top(int limit) {
        Map<String, Integer> out = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit).forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }
}
