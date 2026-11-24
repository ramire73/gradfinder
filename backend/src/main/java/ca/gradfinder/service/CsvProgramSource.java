package ca.gradfinder.service;

import ca.gradfinder.model.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CSV files that were made by crawling a website earlier: one data/csv/(university name).csv per university,
 * with the base columns of ProgramFields.
 * These files are only read. Their values are the base; a crawled page with the same URL replaces them.
 */
@Component
@Order(1)
public class CsvProgramSource implements ProgramSource {
    private static final Logger log = LoggerFactory.getLogger(CsvProgramSource.class);
    private final CsvReader reader = new CsvReader();
    private final InputValidator validator;
    private final Path dir;

    public CsvProgramSource(InputValidator validator, @Value("${gradfinder.data-dir}") String dataDir) {
        this.validator = validator;
        this.dir = Path.of(dataDir, "csv");
    }

    @Override public List<ProgramRecord> loadAll() {
        if (!Files.isDirectory(dir)) return List.of();
        List<ProgramRecord> out = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".csv")).sorted().toList()) out.addAll(load(file));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return out;
    }

    private List<ProgramRecord> load(Path file) throws IOException {
        String university = file.getFileName().toString().replaceFirst("\\.csv$", "");
        List<Map<String, String>> rows = reader.read(text(file));
        Set<String> unknown = new TreeSet<>(rows.isEmpty() ? Set.of() : rows.getFirst().keySet());
        unknown.removeAll(ProgramFields.headings());
        if (!unknown.isEmpty()) log.warn("{}: columns that are not in the base format were ignored: {}", file, unknown);
        Set<String> used = new HashSet<>();
        List<ProgramRecord> out = new ArrayList<>();
        for (Map<String, String> row : rows) {
            ProgramRecord r = record(university, row, used);
            List<String> problems = validator.problems(r);
            if (problems.isEmpty()) out.add(r);
            else log.warn("Skipped row in {}: {} {}", file, r.title(), problems);
        }
        return out;
    }

    private ProgramRecord record(String university, Map<String, String> row, Set<String> used) {
        Map<String, String> fields = new LinkedHashMap<>();
        ProgramFields.COLUMNS.forEach((key, heading) -> {
            String value = row.getOrDefault(heading, "");
            if (!value.isEmpty()) fields.put(key, value);
        });
        String title = row.getOrDefault(ProgramFields.PROGRAM_NAME, ""), url = row.getOrDefault(ProgramFields.URL, "");
        if (!url.isEmpty() && !used.add(url)) { // several degrees on one page still need their own URL
            url += "#" + title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            used.add(url);
        }
        // There is no page, so the row itself is what can be searched.
        String text = title.isEmpty() ? "" : title + "\n" + university + "\n" + String.join("\n", fields.values());
        return new ProgramRecord(university, title, url, null, fields, text);
    }

    /** Excel saves CSV files as UTF-8 or in the Windows encoding. */
    private String text(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }
}
