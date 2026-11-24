package ca.gradfinder.crawler;

import java.io.IOException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Finds and downloads the program pages of one university.
 * To add a university, write one class that implements this and one that implements
 * {@link ca.gradfinder.parser.ProgramParser}, both with the same code() and both marked @Component.
 */
public interface UniversityCrawler {
    /** Short id, for example "ualberta". Used in file names, so only lower-case letters, digits and hyphens. */
    String code();

    /** Full name. The parser must put the same name in its records. */
    String university();

    /**
     * Visits up to limit program pages and calls onPage with (url, html) for each one.
     * Implementations must follow robots.txt, wait between pages, and stop if the site refuses access.
     *
     * @param done page URLs that are already saved and must be skipped
     * @param log  progress messages shown to the user
     */
    void crawl(int limit, Set<String> done, Consumer<String> log, BiConsumer<String, String> onPage)
            throws IOException, InterruptedException;
}
