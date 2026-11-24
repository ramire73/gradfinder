package ca.gradfinder.parser;

import ca.gradfinder.model.ProgramRecord;
import java.util.List;

/** Turns one program page of one university into program records. */
public interface ProgramParser {
    /** Same id as the {@link ca.gradfinder.crawler.UniversityCrawler} of this university. */
    String code();

    /**
     * Field keys come from {@link ca.gradfinder.model.ProgramFields}; leave out values the page does not give.
     * A page that describes several degrees returns several records. Each record needs its own URL,
     * so add a fragment to the page URL, for example url + "#ma" and url + "#msc".
     *
     * @throws IllegalArgumentException if the page is not a program page; it is then skipped
     */
    List<ProgramRecord> parse(String html, String url, String crawledAt);
}
