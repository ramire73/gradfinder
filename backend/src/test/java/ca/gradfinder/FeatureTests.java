package ca.gradfinder;

import static org.junit.jupiter.api.Assertions.*;

import ca.gradfinder.algorithms.*;
import ca.gradfinder.crawler.RobotsTxt;
import ca.gradfinder.parser.AlbertaParser;
import ca.gradfinder.processing.*;
import ca.gradfinder.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FeatureTests {
    @Test void editDistance() {
        assertEquals(3, new EditDistance().between("kitten", "sitting"));
        assertEquals(4, new EditDistance().between("", "word"));
    }
    @Test void spellingPrefersCloserThenMoreFrequent() {
        var s = new SpellChecker(Map.of("science", 9, "sciences", 20, "biology", 5), new EditDistance());
        assertEquals("science", s.suggest("sciense", 5).getFirst());
        assertTrue(s.suggest("zzzzzzzz", 5).isEmpty());
    }
    @Test void completionOrderedByFrequency() {
        Trie t = new Trie();
        t.insert("computer", 2); t.insert("computing", 7); t.insert("biology", 9);
        assertEquals(List.of("computing", "computer"), t.complete("Comp", 5));
        assertTrue(t.complete("xyz", 5).isEmpty());
    }
    @Test void countsWholeWordsOnly() {
        assertEquals(2, new WordCounter().count("Art, arts and ART. smart", "art"));
        assertEquals(2, new WordCounter().count("aa aaa aa", "aa"));
    }
    @Test void indexIntersectsAndSums() {
        var i = new InvertedIndex(new Tokenizer());
        i.add(0, "data science data"); i.add(1, "data"); i.add(2, "science");
        assertEquals(Map.of(0, 3), i.findAll(List.of("data", "science")));
        assertTrue(i.findAll(List.of("data", "missing")).isEmpty());
    }
    @Test void rankingKeepsHighest() {
        assertEquals(List.of(3, 1), new PageRanker().top(Map.of(1, 5, 2, 1, 3, 9, 4, 5), 2).stream().map(PageRanker.Ranked::documentId).toList());
    }
    @Test void validation() {
        assertTrue(new InputValidator().isValid("postalCode", "T6G 2E1"));
        assertFalse(new InputValidator().isValid("postalCode", "D6G 2E1"));
        assertTrue(new InputValidator().isValid("phone", "(780) 492-3111"));
        assertFalse(new InputValidator().isValid("email", "a..b@x.ca"));
        assertFalse(new InputValidator().isValid("query", "<script>"));
        assertFalse(new InputValidator().isValid("date", "2027-13-01"));
        assertTrue(new InputValidator().isValid("query", "Master's (data) science"));
    }
    @Test void patterns() {
        assertEquals(Map.of("a.b@ualberta.ca", 1), new PatternFinder().find("email", "Mail a.b@ualberta.ca."));
        assertEquals(Map.of("BIOCH 670", 2), new PatternFinder().find("courseCode", "BIOCH 670, BIOCH 670"));
        assertEquals(Map.of("$1,250.50", 1), new PatternFinder().find("money", "Fee $1,250.50"));
    }
    @Test void robots() {
        var r = new RobotsTxt("User-agent: *\nDisallow: /a/\nAllow: /a/ok\nUser-agent: other\nDisallow: /");
        assertFalse(r.isAllowed("/a/x"));
        assertTrue(r.isAllowed("/a/ok"));
        assertTrue(r.isAllowed("/b"));
    }
    @Test void parsesAlbertaMarkup() {
        String html = """
            <main><h1>Master of X (Y)</h1><div><h3>Why study this program?</h3><p>Because.</p><h3>Entrance Requirements</h3><p>GPA 3.0</p></div>
            <div><p></p><dt>Faculty:</dt> Arts <p></p><dt>Degree Level:</dt> Master's <p></p></div>
            <div class="accordion-item"><button>Tuition and Fees</button><div class="accordion-body">See fees</div></div>
            <p>Program-specific inquiries email x@ualberta.ca</p><p>General inquiries gps@ualberta.ca</p></main>""";
        var f = new AlbertaParser().parse(html, "https://www.ualberta.ca/x.html", "now").getFirst().fields();
        assertEquals("Arts", f.get("faculty"));
        assertEquals("Master's", f.get("degreeLevel"));
        assertEquals("Because.", f.get("description"));
        assertEquals("GPA 3.0", f.get("entranceRequirements"));
        assertEquals("See fees", f.get("tuitionAndFees"));
        assertEquals("x@ualberta.ca", f.get("contactEmail"));
        assertFalse(f.containsKey("programType"));
    }
    @Test void parsesAgreedColumns() {
        String html = """
            <main><h1>Master of Arts in Old Things (History)</h1>
            <div><p></p><dt>Program Type:</dt> Course Based, Thesis Based <p></p></div>
            <div><h3>Length of Program</h3>
            <p>For the thesis-based program, time varies; however, a minimum of two years is normally required.</p>
            <p>The maximum time to complete the thesis-based MA program as set by the Faculty is four years.</p>
            <p>The maximum time to complete the course-based MA program as set by the Faculty is six years.</p></div>
            <div class="accordion-item"><button>Tuition and Fees</button><div class="accordion-body">
            Program Application Fee (one time): $135.00 Estimated Total Program Tuition (2026-27): $9,916.32</div></div></main>""";
        var f = new AlbertaParser().parse(html, "https://www.ualberta.ca/x.html", "now").getFirst().fields();
        assertEquals("Old Things", f.get("specialization"));
        assertEquals("History", f.get("academicUnit"));
        assertEquals("Coursework OR Thesis", f.get("programComponents"));
        assertEquals("2 years (thesis-based)", f.get("minTimeToCompletion"));
        assertEquals("4 years (thesis-based); 6 years (course-based)", f.get("maxTimeToCompletion"));
        assertEquals("$135.00", f.get("applicationFeeDomestic"));
        assertFalse(f.containsKey("tuitionPerYearDomestic")); // a program total is not a yearly amount
    }
    @Test void thesisProgramsWithCourses() {
        String page = """
            <main><h1>Doctor of Philosophy (X)</h1><div><p></p><dt>Program Type:</dt> Thesis Based <p></p></div>
            <div class="accordion-item"><button>Program Requirements</button><div class="accordion-body">%s</div></div></main>""";
        var p = new AlbertaParser();
        assertEquals("Coursework + Thesis", p.parse(page.formatted("Students take two graduate courses."), "https://a.ca/x", "now").getFirst().fields().get("programComponents"));
        assertEquals("Thesis", p.parse(page.formatted("A dissertation. Courses Graduate courses can be found in Course Listings."), "https://a.ca/x", "now").getFirst().fields().get("programComponents"));
    }
    @Test void admissionDatesPerTerm() {
        String html = """
            <main><h1>Certificate in X</h1><p>Fall 2026 Admission</p><ul><li>Fall Application Deadline: August 15, 2026</li>
            <li>Fall Program Start: September 1, 2026</li></ul>
            <p>Winter 2027 Admission<br>Applications Open: August 25, 2026<br>Applications Close: December 15, 2026</p></main>""";
        var f = new AlbertaParser().parse(html, "https://www.ualberta.ca/x.html", "now").getFirst().fields();
        assertEquals("Fall 2026; Winter 2027", f.get("intake"));
        assertEquals("August 25, 2026 (Winter 2027)", f.get("applicationOpenDate"));
        assertEquals("August 15, 2026 (Fall 2026); December 15, 2026 (Winter 2027)", f.get("canadianApplicationDeadline"));
        assertFalse(f.containsKey("canadianTranscriptDeadline"));
    }
    @Test void moneyKeepsItsScale() {
        assertEquals(Map.of("$35 million", 1, "$1.5 billion", 1, "$35", 1),
                new PatternFinder().find("money", "$35 million, $1.5 billion and $35 millions"));
    }
    @Test void filtersMatchEachValueAndHistorySkipsUnknownWords(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var store = new ProgramStore(json, new InputValidator(), dir.toString());
        store.save("t", List.of(
                new ca.gradfinder.model.ProgramRecord("U", "A", "https://u.ca/a", "now",
                        Map.of("programType", "Course Based, Thesis Based", "deliveryMode", "On Campus or Online", "faculty", "Arts, Law"), "research"),
                new ca.gradfinder.model.ProgramRecord("U", "B", "https://u.ca/b", "now",
                        Map.of("programType", "Thesis Based", "deliveryMode", "Online"), "research")));
        var history = new SearchHistory(json, dir.toString());
        var engine = new SearchEngine(new ProgramDataset(List.of(store), new ProgramProcessor()), history, new PatternFinder());
        engine.reload();
        assertEquals(2, engine.search("", Map.of("programType", "Thesis Based"), 0, 10, false).total());
        assertEquals(1, engine.search("", Map.of("programType", "Course Based"), 0, 10, false).total());
        assertEquals(2, engine.search("", Map.of("deliveryMode", "Online"), 0, 10, false).total());
        assertEquals(Set.of("Course Based", "Thesis Based"), engine.filterOptions().get("programType"));
        assertEquals(Set.of("Arts, Law"), engine.filterOptions().get("faculty"));
        engine.search("reserch", Map.of(), 0, 10, true);
        engine.search("research zzzz", Map.of(), 0, 10, true);
        assertEquals(Map.of("research", 1), history.top(5));
    }
    /** A new university only needs these two classes. The page has two degrees. */
    static class FakeCrawler implements ca.gradfinder.crawler.UniversityCrawler {
        Set<String> skipped;
        public String code() { return "fake"; }
        public String university() { return "Fake University"; }
        public void crawl(int limit, Set<String> done, java.util.function.Consumer<String> log,
                          java.util.function.BiConsumer<String, String> onPage) {
            skipped = done;
            if (!done.contains("https://fake.ca/programs/history/")) onPage.accept("https://fake.ca/programs/history/", "<p>history</p>");
        }
    }
    static class FakeParser implements ca.gradfinder.parser.ProgramParser {
        public String code() { return "fake"; }
        public List<ca.gradfinder.model.ProgramRecord> parse(String html, String url, String at) {
            return java.util.stream.Stream.of("ma", "phd").map(d -> new ca.gradfinder.model.ProgramRecord(
                    "Fake University", d, url + "#" + d, at, Map.of(), "history " + d)).toList();
        }
    }
    @Test void newUniversityOnlyImplementsTheInterfaces(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var store = new ProgramStore(json, new InputValidator(), dir.toString());
        var dataset = new ProgramDataset(List.of(store), new ProgramProcessor());
        var exporter = new UniversityCsvExporter(dataset, dir.toString());
        var engine = new SearchEngine(dataset, new SearchHistory(json, dir.toString()), new PatternFinder());
        var crawler = new FakeCrawler();
        var service = new ca.gradfinder.crawler.CrawlService(store, engine, exporter, List.of(crawler), List.of(new FakeParser()), dir.toString());
        assertEquals(Map.of("fake", "Fake University"), service.universities());
        assertThrows(IllegalArgumentException.class, () -> service.start("other", 1, false));
        for (int run = 0; run < 2; run++) {
            assertTrue(service.start("fake", 5, false));
            for (int i = 0; i < 100 && (boolean) service.status().get("running"); i++) Thread.sleep(50);
            assertEquals(2, store.loadAll().size());
            assertEquals(2, engine.search("history", Map.of(), 0, 10, false).total());
        }
        assertEquals(Set.of("https://fake.ca/programs/history/"), crawler.skipped);
        assertTrue(java.nio.file.Files.exists(dir.resolve("html/fake/history.html")));
        var csv = new CsvReader().read(java.nio.file.Files.readString(dir.resolve("output/fake_programs.csv")));
        assertEquals(2, csv.size());
        assertEquals(ca.gradfinder.model.ProgramFields.headings(), List.copyOf(csv.getFirst().keySet()));
    }
    @Test void crawlerWithoutParserStopsAtStartup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        assertThrows(IllegalStateException.class, () ->
                new ca.gradfinder.crawler.CrawlService(null, null, null, List.of(new FakeCrawler()), List.of(), dir.toString()));
    }
    @Test void spellCheckerAcceptsAnyDistance() {
        StringDistance sameLength = (a, b) -> Math.abs(a.length() - b.length());
        assertEquals(List.of("xyz"), new SpellChecker(Map.of("xyz", 1), sameLength).suggest("abc", 5));
    }
    @Test void moneyInOneFormat() {
        var m = new MoneyNormalizer();
        assertEquals("19590.00", m.normalize("$19,590 ").value());
        assertEquals("134.64", m.normalize("$134.64 CAD").value());
        assertEquals("", m.normalize("$134.64 CAD").note());
        assertEquals(new NormalizedValue("7600.00", "2026-27 Fall/Winter; full-time", ""), m.normalize("$7,600.00 (2026-27 Fall/Winter; full-time)"));
        assertEquals("more than one amount", m.normalize("$40,308.67 domestic; $67,116.67 international").problem());
        assertEquals("$35 million", m.normalize("$35 million").value());
        assertFalse(m.normalize("US$100").problem().isEmpty());
        assertFalse(m.normalize("varies").problem().isEmpty());
    }
    @Test void datesInOneFormat() {
        var d = new DateNormalizer();
        assertEquals("2026-11-02", d.normalize("2-Nov-26").value());
        assertEquals("2026-08-15 (Fall 2026); 2026-12-15 (Winter 2027)", d.normalize("August 15, 2026 (Fall 2026); December 15, 2026 (Winter 2027)").value());
        assertEquals("2027-01-08 (SGS payment)", d.normalize("January 8, 2027 (SGS payment)").value());
        assertEquals("2027-01-08", d.normalize("2027-01-08").value());
        for (String unclear : List.of("March 31 (year not stated)", "31-Feb-27", "01/02/2027", "Within ten days after submission")) {
            assertEquals(unclear, d.normalize(unclear).value());
            assertFalse(d.normalize(unclear).problem().isEmpty());
        }
    }
    @Test void intakesInOneFormat() {
        var i = new IntakeNormalizer();
        assertEquals("Fall 2027", i.normalize("Sep-27").value());
        assertEquals("Summer 2027", i.normalize("May-27").value());
        assertEquals("Fall 2026; Winter 2027", i.normalize("Fall 2026; Winter 2027").value());
        assertFalse(i.normalize("September (annual; year not specified)").problem().isEmpty());
    }
    @Test void universitiesAreCombinedIntoOneDataset(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var store = new ProgramStore(tools.jackson.databind.json.JsonMapper.builder().build(), new InputValidator(), dir.toString());
        store.save("toronto", List.of(new ca.gradfinder.model.ProgramRecord("University of Toronto", "Master of Arts in X (MA)", "https://t.ca/x#ma", "now",
                Map.of("deliveryMode", "In person; Online, Hybrid", "intake", "Sep-27", "canadianApplicationDeadline", "2-Nov-26",
                        "applicationFeeDomestic", "$250.00 ", "tuitionDomesticFirstYear", "$7,600.00 (2026-27)"), "x")));
        store.save("alberta", List.of(new ca.gradfinder.model.ProgramRecord("University of Alberta", "Certificate in X", "https://a.ca/x", "now",
                Map.of("deliveryMode", "On Campus or Online", "intake", "Fall 2027", "canadianApplicationDeadline", "November 2, 2026",
                        "applicationFeeDomestic", "$135.00", "medianFunding", "varies"), "x, \"y\"")));
        var dataset = new ProgramDataset(List.of(store), new ProgramProcessor());
        dataset.reload();
        var rows = dataset.rows();
        var alberta = rows.get(0);
        var toronto = rows.get(1);
        assertEquals(List.copyOf(alberta.keySet()), List.copyOf(toronto.keySet()));
        assertTrue(alberta.keySet().containsAll(ca.gradfinder.model.ProgramFields.COLUMNS.values()));
        for (String same : List.of("Intake", "Canadian Application Deadline")) assertEquals(alberta.get(same), toronto.get(same));
        assertEquals("2026-11-02", toronto.get("Canadian Application Deadline"));
        assertEquals("In person; Online", alberta.get("Mode of Delivery"));
        assertEquals("In person; Online; Hybrid", toronto.get("Mode of Delivery"));
        assertEquals("250.00", toronto.get("Application Fee Domestic"));
        assertEquals("Master's", toronto.get("Degree Level"));
        assertEquals("Tuition Domestic (First Year): 2026-27", toronto.get("Notes"));
        assertEquals("", alberta.get("Program ID"));
        assertEquals(Map.of("University of Alberta", 1, "University of Toronto", 1), dataset.report().programsPerUniversity());
        assertEquals("Median Funding", dataset.report().issues().getFirst().column());
        assertEquals("$7,600.00 (2026-27)", store.loadAll().stream().filter(r -> r.university().endsWith("Toronto")).findFirst().orElseThrow()
                .fields().get("tuitionDomesticFirstYear")); // the original is kept
    }
    @Test void csvIsSafeAndQuoted() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("a", "x, \"y\""); row.put("b", "=1+1"); row.put("c", "é");
        assertEquals("\uFEFFa,b,c\r\n\"x, \"\"y\"\"\",'=1+1,é\r\n", new CsvWriter().write(List.of(row)));
    }
    @Test void percentYearsAndEnrolment() {
        assertEquals("100%", new PercentNormalizer().normalize("100.00%").value());
        assertEquals("82.3%", new PercentNormalizer().normalize("82.30%").value());
        assertFalse(new PercentNormalizer().normalize("140%").problem().isEmpty());
        assertEquals("1 year", new YearsNormalizer().normalize("1 years").value());
        assertEquals("2.7 years", new YearsNormalizer().normalize("2.7").value());
        assertEquals("4 years (thesis-based); 6 years (course-based)", new YearsNormalizer().normalize("4 years (thesis-based); 6 years (course-based)").value());
        assertEquals("2024", new EnrolmentYearNormalizer().normalize("2024/25").value());
        assertEquals("0.00", new MoneyNormalizer().normalize("No application fee").value());
        String funding = "50% of students receive $28,653 or more funding per year in the first 2 years, before tuition and fees";
        assertEquals(new NormalizedValue("28653.00", funding, ""), new MoneyNormalizer().normalize(funding));
    }
    @Test void csvReader() {
        assertEquals(List.of(Map.of("a", "x, \"y\"", "b", "line\nbreak")), new CsvReader().read("\uFEFFa,b\r\n\"x, \"\"y\"\"\",\"line\nbreak\"\r\n,\r\n"));
    }
    @Test void csvBaseIsCombinedWithCrawledData(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Files.createDirectories(dir.resolve("csv"));
        java.nio.file.Files.writeString(dir.resolve("csv/University X.csv"),
                "Specialization,Program Name,URL,Mode of Delivery,Program Components,Graduation Rate\n"
                + "A,Master of A,https://x.ca/a,In person at X Town,Coursework only,90.00%\n"
                + "B,Master of B (MA),https://x.ca/b,Online,Coursework + Options,\n"
                + "B,Master of B (MSc),https://x.ca/b,Online,Coursework + Thesis required,\n"
                + "C,,https://x.ca/c,Online,,\n");
        var store = new ProgramStore(tools.jackson.databind.json.JsonMapper.builder().build(), new InputValidator(), dir.toString());
        store.save("x", List.of(new ca.gradfinder.model.ProgramRecord("University X", "Master of A", "https://x.ca/a", "now",
                Map.of("specialization", "New A", "programComponents", "Coursework"), "page")));
        var dataset = new ProgramDataset(List.of(new CsvProgramSource(new InputValidator(), dir.toString()), store), new ProgramProcessor());
        dataset.reload();
        var rows = dataset.rows();
        assertEquals(3, rows.size()); // the row without a name is skipped
        var a = rows.getFirst();
        assertEquals("New A", a.get("Specialization"));  // crawled value wins
        assertEquals("90%", a.get("Graduation Rate"));   // base value fills the gap
        assertEquals("In person", a.get("Mode of Delivery"));
        assertEquals("Mode of Delivery: at X Town", a.get("Notes"));
        assertEquals("Coursework only", a.get("Program Components"));
        assertEquals("Course Based", a.get("Program Type"));
        assertEquals("https://x.ca/b#master-of-b-msc", rows.get(2).get("URL"));
        assertEquals("Thesis Based", rows.get(2).get("Program Type"));
        assertEquals("", rows.get(1).get("Program Type"));
    }
    @Test void ubcFileIsTheBaseFormat() throws Exception {
        String csv = java.nio.file.Files.readString(java.nio.file.Path.of("data/csv/University of British Columbia.csv"));
        String header = csv.lines().findFirst().orElseThrow().replace("\uFEFF", "");
        assertEquals(List.of(header.split(",")), ca.gradfinder.model.ProgramFields.headings());
        assertEquals(new CsvReader().read(csv).size(), new CsvProgramSource(new InputValidator(), "data").loadAll().stream()
                .filter(r -> r.university().equals("University of British Columbia")).count());
    }
}
