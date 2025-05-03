package com.eulerity.hackathon.imagefinder.service;

import com.eulerity.hackathon.imagefinder.config.CrawlerConfig;
import com.eulerity.hackathon.imagefinder.parser.RobotsTxtParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.*;

/**
 * A multi-threaded service that crawls a website to extract image URLs while
 * respecting robots.txt rules and crawler configuration constraints.
 */
public class ImageCrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(ImageCrawlerService.class);

    private static final int THREAD_COUNT = CrawlerConfig.getThreadCount();
    private static final int POLITENESS_DELAY = CrawlerConfig.getPolitenessDelay();
    private static final int MAX_PAGES = CrawlerConfig.getMaxPages();
    private static final int HTTP_TIMEOUT = CrawlerConfig.getHttpTimeout();
    private static final int MAX_IMAGES = CrawlerConfig.getMaxPages() * 5;

    private final RobotsTxtParser parser = new RobotsTxtParser();

    /**
     * Initiates a crawl starting from the given URL, collecting image sources.
     *
     * @param startUrl the URL to start crawling from
     * @return a set of absolute image URLs found on the domain
     * @throws CrawlLimitExceededException if crawl limits are breached
     */
    public Set<String> crawl(String startUrl) throws CrawlLimitExceededException {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        Set<String> images = ConcurrentHashMap.newKeySet();
        Set<String> disallowed = parser.parse(startUrl);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CompletionService<Void> cs = new ExecutorCompletionService<>(executor);

        visited.add(startUrl);
        cs.submit(() -> {
            crawlPage(startUrl, visited, images, disallowed, cs);
            return null;
        });

        int submitted = 1;
        try {
            for (int i = 0; i < submitted; i++) {
                Future<Void> f = cs.take();
                try {
                    f.get();
                } catch (ExecutionException ee) {
                    if (ee.getCause() instanceof CrawlLimitExceededException) {
                        throw (CrawlLimitExceededException) ee.getCause();
                    }
                    logger.warn("Error during crawl task", ee.getCause());
                }

                if (visited.size() > MAX_PAGES) {
                    throw new CrawlLimitExceededException(
                            "Visited pages exceeded limit of " + MAX_PAGES);
                }
                if (images.size() > MAX_IMAGES) {
                    throw new CrawlLimitExceededException(
                            "Collected images exceeded limit of " + MAX_IMAGES);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CrawlLimitExceededException("Crawl interrupted");
        } finally {
            executor.shutdownNow();
        }

        logger.info("Crawl complete: {} images from {} pages", images.size(), visited.size());
        return images;
    }

    /**
     * Crawls a single page, collects images, and enqueues same-domain links for further crawling.
     *
     * @param url        the URL of the page to crawl
     * @param visited    the set of already visited URLs
     * @param images     the set of collected image URLs
     * @param disallowed disallowed paths from robots.txt
     * @param cs         the thread pool's completion service
     * @throws CrawlLimitExceededException if limits on pages or images are exceeded
     */
    private void crawlPage(String url,
                           Set<String> visited,
                           Set<String> images,
                           Set<String> disallowed,
                           CompletionService<Void> cs) throws CrawlLimitExceededException {
        try {
            Thread.sleep(POLITENESS_DELAY);

            URI uri = new URI(url);
            String path = uri.getPath();
            for (String d : disallowed) {
                if (path.startsWith(d)) return;
            }

            Connection.Response resp = Jsoup.connect(url)
                    .userAgent(CrawlerConfig.getUserAgent())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .timeout(HTTP_TIMEOUT)
                    .execute();

            String contentType = resp.contentType();
            if (resp.statusCode() != 200 || contentType == null || !contentType.contains("text/html")) {
                return;
            }

            Document doc = resp.parse();

            for (Element img : doc.select("img[src]")) {
                if (images.size() >= MAX_IMAGES) {
                    throw new CrawlLimitExceededException(
                            "Collected images exceeded limit of " + MAX_IMAGES);
                }
                String src = img.absUrl("src");
                if (!src.isEmpty()) {
                    images.add(src);
                }
            }

            for (Element link : doc.select("a[href]")) {
                if (visited.size() >= MAX_PAGES) {
                    throw new CrawlLimitExceededException(
                            "Visited pages exceeded limit of " + MAX_PAGES);
                }
                String href = link.absUrl("href");
                if (href.isEmpty() || !visited.add(href)) continue;

                URI next = new URI(href);
                if (!next.getHost().equalsIgnoreCase(uri.getHost())) continue;

                cs.submit(() -> {
                    crawlPage(href, visited, images, disallowed, cs);
                    return null;
                });
            }
        } catch (CrawlLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            logger.debug("Skipping {} due to error: {}", url, e.getMessage());
        }
    }
}
