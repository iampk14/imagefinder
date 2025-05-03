package com.eulerity.hackathon.imagefinder.web;

import com.eulerity.hackathon.imagefinder.service.ImageCrawlerService;
import com.eulerity.hackathon.imagefinder.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servlet that accepts a URL input and returns a list of images found at that URL (and sub-pages)
 * as a JSON response. Optionally filters by image file types.
 */
@WebServlet(name = "ImageFinderServlet", urlPatterns = {"/main"})
public class ImageFinderServlet extends HttpServlet {

    private final ImageCrawlerService crawler = new ImageCrawlerService();

    /**
     * Handles CORS preflight requests. Actual headers are added by {@link CORSFilter}.
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Handles POST requests to crawl a provided URL for images.
     *
     * @param req  the HttpServletRequest containing parameters:
     *             - url: the target URL to crawl (required)
     *             - fileTypes: optional comma-separated list of allowed image extensions
     * @param resp the HttpServletResponse containing a JSON response of image URLs
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during response writing
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        String rawUrl = req.getParameter("url");
        String typesParam = req.getParameter("fileTypes");

        Set<String> allowedExts = (typesParam == null || typesParam.trim().isEmpty())
                ? Collections.emptySet()
                : Arrays.stream(typesParam.split(","))
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        // 1) Validate 'url' parameter
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            writeError(resp, "Missing 'url' parameter.");
            return;
        }

        // 2) Validate URL format and protocol
        URL url;
        try {
            url = new URL(rawUrl);
            String protocol = url.getProtocol().toLowerCase();
            if (!("http".equals(protocol) || "https".equals(protocol))) {
                throw new MalformedURLException("Unsupported protocol");
            }
        } catch (MalformedURLException e) {
            writeError(resp, "Invalid URL format.");
            return;
        }

        // 3) Crawl for image URLs
        Set<String> rawImages;
        try {
            rawImages = crawler.crawl(url.toExternalForm());
        } catch (Exception e) {
            writeError(resp, "Server error crawling URL: " + e.getMessage());
            return;
        }

        // 4) Use fallback images if none found
        if (rawImages.isEmpty()) {
            rawImages = new LinkedHashSet<>(Arrays.asList(ImageStreamServlet.TEST_IMAGES));
        }

        // 5) Filter image URLs by extension and mark likely logos
        List<ImageResponse.Item> items = rawImages.stream()
            .filter(u -> {
                if (allowedExts.isEmpty()) return true;
                String lower = u.toLowerCase();
                return allowedExts.stream().anyMatch(ext -> lower.endsWith("." + ext));
            })
            .map(u -> new ImageResponse.Item(u, likelyLogo(u)))
            .collect(Collectors.toList());

        // 6) Return response as JSON
        ImageResponse out = new ImageResponse(items, null);
        resp.getWriter().write(JsonUtil.toJson(out));
    }

    /**
     * Writes an error message as a JSON response with HTTP 400 status.
     *
     * @param resp the HTTP response object
     * @param msg  the error message
     * @throws IOException if response writing fails
     */
    private void writeError(HttpServletResponse resp, String msg) throws IOException {
        ImageResponse out = new ImageResponse(Collections.emptyList(), msg);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write(JsonUtil.toJson(out));
    }

    /**
     * Very basic heuristic to detect logo images based on file names.
     *
     * @param url the image URL
     * @return true if the filename likely refers to a logo
     */
    private boolean likelyLogo(String url) {
        String path = url.substring(url.lastIndexOf('/') + 1).toLowerCase();
        return path.contains("logo");
    }
}
