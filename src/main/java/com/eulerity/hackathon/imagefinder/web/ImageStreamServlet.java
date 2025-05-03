package com.eulerity.hackathon.imagefinder.web;

import com.eulerity.hackathon.imagefinder.config.CrawlerConfig;
import com.eulerity.hackathon.imagefinder.parser.RobotsTxtParser;
import com.eulerity.hackathon.imagefinder.util.JsonUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servlet that performs asynchronous image crawling and streams results via Server-Sent Events (SSE).
 * Responds to GET requests on the /stream endpoint.
 */
@WebServlet(name = "ImageStream", urlPatterns = {"/stream"}, asyncSupported = true)
public class ImageStreamServlet extends HttpServlet {

    /** Fallback static images in case of an empty crawl result. */
    public static final String[] TEST_IMAGES = {
            "https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg",
            "https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg",
            "https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg",
            "https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg"
    };

    private static final int THREAD_COUNT = CrawlerConfig.getThreadCount();
    private static final int TIMEOUT = CrawlerConfig.getHttpTimeout();
    private static final int POLITENESS = CrawlerConfig.getPolitenessDelay();
    private static final int MAX_DEPTH = CrawlerConfig.getMaxDepth();

    private final RobotsTxtParser parser = new RobotsTxtParser();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Starts an asynchronous crawl for the given URL and streams image results back to the client.
     *
     * @param req  HTTP request with a 'url' parameter
     * @param resp SSE-compatible HTTP response
     * @throws ServletException on servlet error
     * @throws IOException on I/O failure
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String startUrl = req.getParameter("url");
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        if (startUrl == null || startUrl.trim().isEmpty() || !isHttpUrl(startUrl)) {
            sendEvent(out, "error", "Missing or invalid URL parameter.");
            return;
        }

        AsyncContext ac = req.startAsync();
        ac.setTimeout(0);

        Set<String> disallowed = parser.parse(startUrl);
        Set<String> visited = ConcurrentHashMap.newKeySet();
        Set<String> images = ConcurrentHashMap.newKeySet();
        AtomicInteger tasks = new AtomicInteger(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        visited.add(startUrl);
        executor.submit(new StreamingCrawlTask(
                startUrl, visited, images, disallowed, executor, tasks, out, ac, 0));
    }

    /**
     * Sends a named SSE event to the client.
     *
     * @param out   the PrintWriter for the response stream
     * @param event the event name
     * @param data  the event payload
     */
    private void sendEvent(PrintWriter out, String event, String data) {
        out.write("event: " + event + "\n");
        out.write("data: " + data + "\n\n");
        out.flush();
    }

    /**
     * Validates if the provided string is a valid HTTP/HTTPS URL.
     *
     * @param url the input string
     * @return true if it's a valid HTTP(S) URL
     */
    private boolean isHttpUrl(String url) {
        String l = url.toLowerCase();
        return l.startsWith("http://") || l.startsWith("https://");
    }

    /**
     * Internal task class for crawling individual pages and streaming image discoveries via SSE.
     */
    private class StreamingCrawlTask implements Runnable {
        private final String url;
        private final Set<String> visited, images, disallowed;
        private final ExecutorService executor;
        private final AtomicInteger tasks;
        private final PrintWriter out;
        private final AsyncContext ac;
        private final int depth;

        StreamingCrawlTask(String url,
                           Set<String> visited,
                           Set<String> images,
                           Set<String> disallowed,
                           ExecutorService executor,
                           AtomicInteger tasks,
                           PrintWriter out,
                           AsyncContext ac,
                           int depth) {
            this.url = url;
            this.visited = visited;
            this.images = images;
            this.disallowed = disallowed;
            this.executor = executor;
            this.tasks = tasks;
            this.out = out;
            this.ac = ac;
            this.depth = depth;
        }

        @Override
        public void run() {
            try {
                if (depth > MAX_DEPTH) return;
                Thread.sleep(POLITENESS);

                URL u = new URL(url);
                String path = u.getPath();
                for (String d : disallowed)
                    if (path.startsWith(d)) return;

                Connection.Response resp = Jsoup.connect(url)
                        .userAgent(RobotsTxtParser.DEFAULT_AGENT)
                        .timeout(TIMEOUT)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .execute();

                String contentType = resp.contentType();
                if (resp.statusCode() != 200 || contentType == null || !contentType.contains("text/html")) {
                    return;
                }

                Document doc = resp.parse();

                // Stream discovered images
                for (Element img : doc.select("img[src]")) {
                    String src = img.absUrl("src");
                    if (!isHttpUrl(src)) continue;
                    if (images.add(src)) {
                        ImageResponse.Item item = new ImageResponse.Item(src, likelyLogo(src));
                        out.write("data: " + JsonUtil.toJson(item) + "\n\n");
                        out.flush();
                    }
                }

                // Recurse on same-domain links
                for (Element link : doc.select("a[href]")) {
                    String href = link.absUrl("href");
                    if (!isHttpUrl(href) || !visited.add(href)) continue;

                    URL next = new URL(href);
                    if (!next.getHost().equals(u.getHost())) continue;

                    String np = next.getPath();
                    if (disallowed.stream().anyMatch(np::startsWith)) continue;

                    tasks.incrementAndGet();
                    executor.submit(new StreamingCrawlTask(
                            href, visited, images, disallowed, executor, tasks, out, ac, depth + 1));
                }

            } catch (Exception ignored) {
            } finally {
                if (tasks.decrementAndGet() == 0) {
                    sendEvent(out, "done", "");
                    executor.shutdown();
                    ac.complete();
                }
            }
        }

        private boolean isHttpUrl(String u) {
            String l = u.toLowerCase();
            return l.startsWith("http://") || l.startsWith("https://");
        }

        private boolean likelyLogo(String url) {
            String p = url.substring(url.lastIndexOf('/') + 1).toLowerCase();
            return p.contains("logo");
        }
    }
}
