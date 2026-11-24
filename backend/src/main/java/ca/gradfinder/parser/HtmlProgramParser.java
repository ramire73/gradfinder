package ca.gradfinder.parser;

import ca.gradfinder.model.ProgramRecord;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

/**
 * Steps shared by parsers of pages with one program (template method pattern). Subclasses supply the university
 * and the fields. For pages with several programs, implement {@link ProgramParser} directly.
 */
public abstract class HtmlProgramParser implements ProgramParser {
    protected abstract String university();

    /** Values found on the page. Keys come from {@link ca.gradfinder.model.ProgramFields}. */
    protected abstract Map<String, String> extractFields(Element main, String title);

    @Override public final List<ProgramRecord> parse(String html, String url, String crawledAt) {
        Document doc = Jsoup.parse(html, url);
        Element main = doc.selectFirst("main");
        if (main == null) throw new IllegalArgumentException("No <main> element in " + url);
        main.select("script, style, nav, [style~=display:\\s*none]").remove();
        Element h1 = main.selectFirst("h1");
        if (h1 == null) throw new IllegalArgumentException("No title in " + url);
        String title = h1.text();
        Map<String, String> fields = extractFields(main, title);
        main.select("h1").forEach(Element::remove); // the title is added once below
        return List.of(new ProgramRecord(university(), title, url, crawledAt, fields, title + "\n" + main.text()));
    }

    /** Adds a value only if the page gives one. */
    protected final void put(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) fields.put(key, value.strip());
    }

    /** Text after a heading, up to the next heading of the same or a higher level. */
    protected final String afterHeading(Element main, String name) {
        for (Element h : main.select("h2, h3, h4")) {
            if (!h.text().strip().equalsIgnoreCase(name)) continue;
            StringBuilder value = new StringBuilder();
            for (Element e = h.nextElementSibling(); e != null; e = e.nextElementSibling()) {
                if (e.normalName().matches("h[1-6]") && e.normalName().compareTo(h.normalName()) <= 0) break;
                value.append(e.text()).append('\n');
            }
            return value.toString();
        }
        return null;
    }
}
