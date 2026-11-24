package ca.gradfinder.crawler;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

/**
 * Steps shared by every Selenium crawler (template method pattern): open Chrome, obey robots.txt,
 * collect the links, then visit the pages with a delay. Subclasses supply only what differs per website.
 */
public abstract class SeleniumCrawler implements UniversityCrawler {
    private final Duration delay;

    protected SeleniumCrawler(int delaySeconds) { this.delay = Duration.ofSeconds(Math.max(1, delaySeconds)); }

    /** For example "https://www.ualberta.ca", without a slash at the end. */
    protected abstract String host();

    /** The page that lists the programs. */
    protected abstract String listUrl();

    /** An element that shows a program page has finished loading. */
    protected abstract By pageReady();

    /** Program URLs to visit, at most limit, without those in done. Must check robots before adding a URL. */
    protected abstract List<String> collectLinks(WebDriver driver, int limit, Set<String> done, RobotsTxt robots,
                                                 Consumer<String> log) throws IOException, InterruptedException;

    /** Override if the site shows a different page when it refuses access. */
    protected boolean isBlocked(WebDriver driver) {
        String title = driver.getTitle();
        return title != null && title.contains("request could not be satisfied");
    }

    @Override public final void crawl(int limit, Set<String> done, Consumer<String> log, BiConsumer<String, String> onPage)
            throws IOException, InterruptedException {
        WebDriver driver = new ChromeDriver(new ChromeOptions());
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            RobotsTxt robots = loadRobots(driver);
            if (!robots.isAllowed(URI.create(listUrl()).getPath())) throw new IOException("robots.txt does not allow the program list");
            pause();
            visit(driver, collectLinks(driver, limit, done, robots, log), log, onPage);
        } finally {
            driver.quit();
        }
    }

    private void visit(WebDriver driver, List<String> urls, Consumer<String> log, BiConsumer<String, String> onPage)
            throws IOException, InterruptedException {
        int n = 0;
        for (String url : urls) {
            pause();
            n++;
            try {
                open(driver, url);
                new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.presenceOfElementLocated(pageReady()));
                log.accept("Page " + n + " of " + urls.size() + ": " + driver.getTitle());
                onPage.accept(url, driver.getPageSource());
            } catch (TimeoutException e) {
                log.accept("Skipped (timed out): " + url);
            }
        }
    }

    protected final void pause() throws InterruptedException { Thread.sleep(delay); }

    /** Opens a page, and stops the crawl if the site refuses. */
    protected final void open(WebDriver driver, String url) throws IOException {
        driver.get(url);
        if (isBlocked(driver)) throw new IOException("The site refused the request (403), so crawling stopped. Try again later.");
    }

    /** Uses the crawling browser itself. If the rules cannot be read, nothing is crawled. */
    private RobotsTxt loadRobots(WebDriver driver) throws IOException {
        open(driver, host() + "/robots.txt");
        String content = driver.findElement(By.tagName("body")).getText();
        if (!content.toLowerCase(Locale.ROOT).contains("user-agent:")) throw new IOException("Could not read robots.txt");
        return new RobotsTxt(content);
    }
}
