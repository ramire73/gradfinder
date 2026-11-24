package ca.gradfinder.processing;

import ca.gradfinder.model.*;
import ca.gradfinder.service.ProgramStore;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * One dataset of all universities in one format. The files in data/programs keep the original values;
 * this is rebuilt from them, so processing rules can change without crawling again.
 */
@Service
public class ProgramDataset {
    public record Issue(String university, String program, String column, String value, String reason) {}
    public record Report(int programs, Map<String, Integer> programsPerUniversity, Map<String, Integer> filledPerColumn, List<Issue> issues) {}

    private final ProgramStore store;
    private final ProgramProcessor processor;
    private volatile List<ProcessedProgram> programs = List.of();

    public ProgramDataset(ProgramStore store, ProgramProcessor processor) { this.store = store; this.processor = processor; }

    public List<ProgramRecord> reload() {
        programs = store.loadAll().stream().map(processor::process)
                .sorted(Comparator.comparing((ProcessedProgram p) -> p.program().university()).thenComparing(p -> p.program().title())
                        .thenComparing(p -> p.program().url())).toList();
        return programs.stream().map(ProcessedProgram::program).toList();
    }

    /** Every row has every column, in the same order. A value the university does not give is empty. */
    public List<Map<String, String>> rows() { return programs.stream().map(this::row).toList(); }

    private Map<String, String> row(ProcessedProgram p) {
        ProgramRecord r = p.program();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("University", r.university());
        row.put("Program Name", r.title());
        row.put("Degree Level", r.fields().getOrDefault("degreeLevel", ""));
        row.put("URL", r.url());
        ProgramFields.COLUMNS.forEach((key, heading) -> row.put(heading, r.fields().getOrDefault(key, "")));
        List<String> notes = new ArrayList<>();
        p.notes().forEach((key, note) -> notes.add(heading(key) + ": " + note));
        row.put("Notes", String.join("; ", notes));
        row.put("Crawled At", r.crawledAt() == null ? "" : r.crawledAt());
        return row;
    }

    public Report report() {
        Map<String, Integer> universities = new TreeMap<>();
        Map<String, Integer> filled = new LinkedHashMap<>();
        for (Map<String, String> row : rows()) {
            universities.merge(row.get("University"), 1, Integer::sum);
            row.forEach((column, value) -> filled.merge(column, value.isEmpty() ? 0 : 1, Integer::sum));
        }
        List<Issue> issues = new ArrayList<>();
        for (ProcessedProgram p : programs)
            p.problems().forEach((key, reason) -> issues.add(new Issue(p.program().university(), p.program().title(),
                    heading(key), p.program().fields().get(key), reason)));
        return new Report(programs.size(), universities, filled, issues);
    }

    private String heading(String key) { return ProgramFields.COLUMNS.getOrDefault(key, key.equals("degreeLevel") ? "Degree Level" : key); }
}
