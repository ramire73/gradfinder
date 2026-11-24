package ca.gradfinder.crawler;

import ca.gradfinder.model.ProgramRecord;
import ca.gradfinder.parser.ProgramParser;
import ca.gradfinder.service.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs one crawl at a time in the background and saves each page as soon as it is read.
 * Spring passes in every UniversityCrawler and ProgramParser, so this class does not change for a new university.
 */
@Service
public class CrawlService {
    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private final ProgramStore store;
    private final SearchEngine engine;
    private final Path htmlDir;
    private final Map<String, UniversityCrawler> crawlers = new TreeMap<>();
    private final Map<String, ProgramParser> parsers = new TreeMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile String message = "Not started";
    private volatile int saved;

    public CrawlService(ProgramStore store, SearchEngine engine, List<UniversityCrawler> crawlers,
                        List<ProgramParser> parsers, @Value("${gradfinder.data-dir}") String dataDir) {
        this.store = store;
        this.engine = engine;
        this.htmlDir = Path.of(dataDir, "html");
        for (UniversityCrawler c : crawlers) {
            if (!c.code().matches("[a-z0-9-]+")) throw new IllegalStateException("Invalid university code: " + c.code());
            if (this.crawlers.put(c.code(), c) != null) throw new IllegalStateException("Two crawlers use the code " + c.code());
        }
        for (ProgramParser p : parsers)
            if (this.parsers.put(p.code(), p) != null) throw new IllegalStateException("Two parsers use the code " + p.code());
        if (!this.crawlers.keySet().equals(this.parsers.keySet()))
            throw new IllegalStateException("Each university needs a crawler and a parser with the same code. Crawlers: "
                    + this.crawlers.keySet() + ", parsers: " + this.parsers.keySet());
    }

    /** Code to name of every university that can be crawled. */
    public Map<String, String> universities() {
        Map<String, String> out = new LinkedHashMap<>();
        crawlers.forEach((code, c) -> out.put(code, c.university()));
        return out;
    }

    public boolean start(String code, int limit, boolean refresh) {
        UniversityCrawler crawler = crawlers.get(code);
        if (crawler == null) throw new IllegalArgumentException("Unknown university: " + code);
        if (!running.compareAndSet(false, true)) return false;
        saved = 0;
        report("Starting: " + crawler.university());
        Thread.ofPlatform().daemon().name("crawler").start(() -> run(crawler, parsers.get(code), limit, refresh));
        return true;
    }

    private void run(UniversityCrawler crawler, ProgramParser parser, int limit, boolean refresh) {
        Map<String, ProgramRecord> records = new LinkedHashMap<>();
        try {
            for (ProgramRecord r : store.loadAll())
                if (crawler.university().equals(r.university())) records.put(r.url(), r);
            Set<String> done = new HashSet<>();
            if (!refresh) records.keySet().forEach(url -> done.add(page(url)));
            Path dir = htmlDir.resolve(crawler.code());
            crawler.crawl(limit, done, this::report, (url, html) -> {
                try {
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve(fileName(url)), html);
                    List<ProgramRecord> parsed = parser.parse(html, url, Instant.now().toString());
                    records.keySet().removeIf(old -> page(old).equals(page(url))); // a degree may have been removed
                    for (ProgramRecord r : parsed) {
                        if (!crawler.university().equals(r.university()))
                            throw new IllegalArgumentException("parser used the university name " + r.university());
                        if (records.put(r.url(), r) != null) log.warn("Two records share the URL {}", r.url());
                    }
                    store.save(crawler.code(), records.values());
                    saved++;
                } catch (IllegalArgumentException e) {
                    log.warn("Could not parse {}: {}", url, e.getMessage());
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            report("Finished: " + saved + " pages saved");
        } catch (Exception e) {
            log.error("Crawl failed", e);
            report("Stopped: " + String.valueOf(e.getMessage()).lines().findFirst().orElse(""));
        } finally {
            engine.reload();
            running.set(false);
        }
    }

    /** Several records can come from one page: they differ only after the #. */
    private static String page(String url) {
        int hash = url.indexOf('#');
        return hash < 0 ? url : url.substring(0, hash);
    }

    /** Last part of the URL path, for example "master-of-arts.html". */
    private static String fileName(String url) {
        String path = page(url).replaceAll("/+$", "");
        String name = path.substring(path.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        return name.endsWith(".html") ? name : name + ".html";
    }

    private void report(String text) { message = text; log.info(text); }

    public Map<String, Object> status() { return Map.of("running", running.get(), "message", message, "saved", saved); }
}
