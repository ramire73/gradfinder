package ca.gradfinder.processing;

import ca.gradfinder.model.*;
import ca.gradfinder.service.ProgramSource;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * One dataset of all universities in the base format. The files in data keep the original values;
 * this is rebuilt from them, so processing rules can change without crawling again.
 */
@Service
public class ProgramDataset {
    public record Issue(String university, String program, String column, String value, String reason) {}
    public record Report(int programs, Map<String, Integer> programsPerUniversity, Map<String, Integer> filledPerColumn, List<Issue> issues) {}

    private final List<ProgramSource> sources;
    private final ProgramProcessor processor;
    private volatile List<ProcessedProgram> programs = List.of();

    public ProgramDataset(List<ProgramSource> sources, ProgramProcessor processor) { this.sources = sources; this.processor = processor; }

    public List<ProgramRecord> reload() {
        programs = combine().stream().map(processor::process)
                .sorted(Comparator.comparing((ProcessedProgram p) -> p.program().university()).thenComparing(p -> p.program().title())
                        .thenComparing(p -> p.program().url())).toList();
        return programs.stream().map(ProcessedProgram::program).toList();
    }

    /** One record per URL. Later sources are newer: their values win, and earlier ones fill the gaps. */
    private Collection<ProgramRecord> combine() {
        Map<String, ProgramRecord> byUrl = new LinkedHashMap<>();
        for (ProgramSource source : sources)
            for (ProgramRecord r : source.loadAll()) byUrl.merge(r.url(), r, this::merge);
        return byUrl.values();
    }

    private ProgramRecord merge(ProgramRecord base, ProgramRecord newer) {
        Map<String, String> fields = new LinkedHashMap<>(base.fields());
        fields.putAll(newer.fields());
        return new ProgramRecord(newer.university(), newer.title(), newer.url(), newer.crawledAt(), fields, newer.text());
    }

    /** Every row has every column, in the same order. A value the university does not give is empty. */
    public List<Map<String, String>> rows() { return programs.stream().map(this::row).toList(); }

    /** The programs of one university with exactly the base columns. */
    public List<Map<String, String>> baseRows(String university) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> row : rows()) {
            if (!row.get("University").equals(university)) continue;
            Map<String, String> base = new LinkedHashMap<>();
            ProgramFields.headings().forEach(heading -> base.put(heading, row.get(heading)));
            out.add(base);
        }
        return out;
    }

    private Map<String, String> row(ProcessedProgram p) {
        ProgramRecord r = p.program();
        Map<String, String> row = new LinkedHashMap<>();
        row.put("University", r.university());
        ProgramFields.headings().forEach(heading -> row.put(heading, "")); // keeps the order of the base format
        ProgramFields.COLUMNS.forEach((key, heading) -> row.put(heading, r.fields().getOrDefault(key, "")));
        row.put(ProgramFields.PROGRAM_NAME, r.title());
        row.put(ProgramFields.URL, r.url());
        row.put("Degree Level", r.fields().getOrDefault("degreeLevel", ""));
        row.put("Program Type", r.fields().getOrDefault("programType", ""));
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

    private String heading(String key) { return ProgramFields.COLUMNS.getOrDefault(key, key.equals("degreeLevel") ? "Degree Level" : key.equals("programType") ? "Program Type" : key); }
}
