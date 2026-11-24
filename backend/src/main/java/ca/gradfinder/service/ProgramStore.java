package ca.gradfinder.service;

import ca.gradfinder.model.ProgramRecord;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Reads every data/programs/*.json file, so each teammate only needs to add their own file. */
@Service
@Order(2)
public class ProgramStore implements ProgramSource {
    private static final Logger log = LoggerFactory.getLogger(ProgramStore.class);
    private final ObjectMapper json;
    private final InputValidator validator;
    private final Path dir;

    public ProgramStore(ObjectMapper json, InputValidator validator, @Value("${gradfinder.data-dir}") String dataDir) {
        this.json = json;
        this.validator = validator;
        this.dir = Path.of(dataDir, "programs");
    }

    @Override public List<ProgramRecord> loadAll() {
        if (!Files.isDirectory(dir)) return List.of();
        Map<String, ProgramRecord> byUrl = new LinkedHashMap<>(); // one record per URL
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                try {
                    for (ProgramRecord r : json.readValue(file.toFile(), ProgramRecord[].class)) {
                        List<String> problems = validator.problems(r);
                        if (problems.isEmpty()) byUrl.put(r.url(), r);
                        else log.warn("Skipped record in {}: {} {}", file, r.url(), problems);
                    }
                } catch (JacksonException e) {
                    log.error("Skipped unreadable file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return List.copyOf(byUrl.values());
    }

    public void save(String code, Collection<ProgramRecord> records) throws IOException {
        Files.createDirectories(dir);
        Path temp = dir.resolve(code + ".json.tmp");
        Files.writeString(temp, json.writerWithDefaultPrettyPrinter().writeValueAsString(records));
        Files.move(temp, dir.resolve(code + ".json"), StandardCopyOption.REPLACE_EXISTING);
    }
}
