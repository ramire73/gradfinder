package ca.gradfinder.crawler;

import ca.gradfinder.parser.AlbertaParser;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Selenium crawler for University of Alberta graduate programs.
 * The program list is drawn by JavaScript (Coveo), so a real browser is needed.
 */
@Component
public final class AlbertaCrawler extends SeleniumCrawler {
    private static final String HOST = "https://www.ualberta.ca";
    private static final String LIST = HOST + "/en/graduate-programs/index.html";
    private static final String PROGRAM_PREFIX = HOST + "/en/graduate-programs/";
    private static final By LINKS = By.cssSelector(".CoveoResult a.CoveoResultLink");
    private static final By TOTAL = By.cssSelector(".coveo-highlight-total-count");

    public AlbertaCrawler(@Value("${gradfinder.crawl-delay-seconds}") int delaySeconds) {
        super(delaySeconds);
    }

    @Override public String code() { return AlbertaParser.CODE; }
    @Override public String university() { return AlbertaParser.UNIVERSITY; }
    @Override protected String host() { return HOST; }
    @Override protected String listUrl() { return LIST; }
    @Override protected By pageReady() { return By.cssSelector("main h1"); }

    @Override protected List<String> collectLinks(WebDriver driver, int limit, Set<String> done, RobotsTxt robots, Consumer<String> log)
            throws InterruptedException, IOException {
        Set<String> seen = new LinkedHashSet<>();
        List<String> wanted = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.ignoring(StaleElementReferenceException.class);
        int offset = 0, total = Integer.MAX_VALUE;
        while (offset < total && wanted.size() < limit) {
            if (offset > 0) pause();
            open(driver, LIST + "#first=" + offset + "&sort=%40ua__grad_sub%20ascending");
            String expected = Integer.toString(offset + 1);
            // Wait for "Results 25-48 of 297". The summary is hidden by CSS, so getText() would be empty.
            wait.until(d -> d.findElement(By.cssSelector(".coveo-highlight-first")).getAttribute("textContent").replace(",", "").equals(expected));
            List<String> hrefs = wait.until(d -> {
                List<String> h = d.findElements(LINKS).stream().map(e -> e.getAttribute("href")).toList();
                return h.isEmpty() ? null : h;
            });
            total = Integer.parseInt(driver.findElement(TOTAL).getAttribute("textContent").replaceAll("\\D", ""));
            for (String url : hrefs)
                if (url.startsWith(PROGRAM_PREFIX) && robots.isAllowed(URI.create(url).getPath())
                        && seen.add(url) && !done.contains(url) && wanted.size() < limit) wanted.add(url);
            offset += hrefs.size();
            log.accept("Read program list: " + Math.min(offset, total) + " of " + total);
        }
        return wanted;
    }
}
