package ca.gradfinder.web;

import ca.gradfinder.crawler.CrawlService;
import ca.gradfinder.processing.*;
import ca.gradfinder.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record CrawlRequest(@NotBlank String university, @Min(1) @Max(1000) int limit, boolean refresh) {}
    public record ValidateRequest(@NotNull String type, @NotNull String value) {}

    private final SearchEngine engine;
    private final SearchHistory history;
    private final CrawlService crawler;
    private final InputValidator validator;
    private final PatternFinder patternFinder;
    private final ProgramDataset dataset;
    private final CsvWriter csv = new CsvWriter();

    public ApiController(SearchEngine engine, SearchHistory history, CrawlService crawler,
                         InputValidator validator, PatternFinder patternFinder, ProgramDataset dataset) {
        this.engine = engine; this.history = history; this.crawler = crawler;
        this.validator = validator; this.patternFinder = patternFinder; this.dataset = dataset;
    }

    @GetMapping("/search")
    public SearchEngine.Result search(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean record,
            @RequestParam(defaultValue = "") String university, @RequestParam(defaultValue = "") String faculty,
            @RequestParam(defaultValue = "") String degreeLevel, @RequestParam(defaultValue = "") String programType,
            @RequestParam(defaultValue = "") String deliveryMode) {
        if (page < 0 || page > 1000) throw new IllegalArgumentException("Invalid page number.");
        if (!q.isBlank()) validator.require("query", q);
        return engine.search(q, Map.of("university", university, "faculty", faculty, "degreeLevel", degreeLevel,
                "programType", programType, "deliveryMode", deliveryMode), page, 10, record);
    }

    @GetMapping("/dataset") public Object dataset() { return dataset.rows(); }
    @GetMapping("/dataset/report") public Object datasetReport() { return dataset.report(); }

    @GetMapping("/dataset.csv")
    public ResponseEntity<byte[]> datasetCsv() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"graduate-programs.csv\"")
                .body(csv.write(dataset.rows()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/filters") public Object filters() { return engine.filterOptions(); }
    @GetMapping("/pages") public Object pages() { return engine.pages(); }
    @GetMapping("/stats") public Object stats() { return engine.stats(); }
    @GetMapping("/history") public Object history() { return history.top(20); }
    @GetMapping("/complete") public Object complete(@RequestParam String prefix) { return engine.complete(validator.require("word", prefix)); }
    @GetMapping("/spell") public Object spell(@RequestParam String word) { return engine.spell(validator.require("word", word)); }
    @GetMapping("/index") public Object index(@RequestParam String word) { return engine.indexLookup(validator.require("word", word)); }

    @GetMapping("/frequency")
    public Object frequency(@RequestParam String word, @RequestParam String url) {
        return engine.frequency(validator.require("word", word), validator.require("url", url));
    }

    @GetMapping("/pattern-types") public Object patternTypes() { return patternFinder.types(); }

    @GetMapping("/patterns")
    public Object patterns(@RequestParam String type, @RequestParam(defaultValue = "") String url) { return engine.patterns(type, url); }

    @GetMapping("/validation-types") public Object validationTypes() { return validator.types(); }

    @PostMapping("/validate")
    public Object validate(@Valid @RequestBody ValidateRequest r) { return Map.of("valid", validator.isValid(r.type(), r.value())); }

    @GetMapping("/crawlers") public Object crawlers() { return crawler.universities(); }
    @GetMapping("/crawl") public Object crawlStatus() { return crawler.status(); }

    @PostMapping("/crawl")
    public ResponseEntity<Object> crawl(@Valid @RequestBody CrawlRequest r) {
        return crawler.start(r.university(), r.limit(), r.refresh()) ? ResponseEntity.accepted().body(crawler.status())
                : ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "A crawl is already running."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Object> bad(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Object> missing(NoSuchElementException e) { return ResponseEntity.status(404).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> invalid(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(Map.of("error", "Some values are missing or out of range.")); }
}
