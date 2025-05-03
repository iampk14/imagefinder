package com.eulerity.hackathon.imagefinder.parser;

import com.eulerity.hackathon.imagefinder.config.CrawlerConfig;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

/**
 * Utility class to parse and cache <code>robots.txt</code> rules for domains.
 * Helps determine whether specific paths are disallowed for crawling.
 */
public class RobotsTxtParser {

    /** Default user agent used to match applicable <code>robots.txt</code> rules. */
    public static final String DEFAULT_AGENT = CrawlerConfig.getUserAgent();

    /** Cache mapping domain names to disallowed path sets. */
    private static final Map<String, Set<String>> cache = new ConcurrentHashMap<>();

    /**
     * Parses the <code>robots.txt</code> file of the given base URL.
     * Caches disallowed paths for future lookups.
     *
     * @param baseUrl the base URL whose domain's <code>robots.txt</code> will be parsed
     * @return a set of disallowed path prefixes for that domain
     */
    public Set<String> parse(String baseUrl) {
        try {
            URL u = new URL(baseUrl);
            String domain = u.getHost().toLowerCase();
            return cache.computeIfAbsent(domain, d -> {
                String content = fetchRobotsTxt(u);
                return parseContent(content, DEFAULT_AGENT);
            });
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * Parses disallowed paths from a <code>robots.txt</code> string for a specific user agent.
     *
     * @param robotsTxtContent the content of the <code>robots.txt</code> file
     * @param userAgent the user agent to match rules against
     * @return a set of disallowed path prefixes
     */
    public static Set<String> parse(String robotsTxtContent, String userAgent) {
        return parseContent(robotsTxtContent, userAgent);
    }

    /**
     * Parses and extracts disallowed paths for the given user agent from content.
     *
     * @param content the raw <code>robots.txt</code> content
     * @param userAgent the user agent for which to apply rules
     * @return a set of disallowed paths for the given user agent
     */
    private static Set<String> parseContent(String content, String userAgent) {
        Set<String> disallowed = new HashSet<>();
        if (content == null) return disallowed;

        boolean includeWildcard = DEFAULT_AGENT.equalsIgnoreCase(userAgent);
        boolean applies = false;

        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.split("#", 2)[0].trim();
            if (line.isEmpty()) {
                applies = false;
                continue;
            }

            String lower = line.toLowerCase();

            if (lower.startsWith("user-agent:")) {
                String ua = line.substring(11).trim();
                applies = includeWildcard
                        ? "*".equals(ua) || ua.equalsIgnoreCase(userAgent)
                        : ua.equalsIgnoreCase(userAgent);
            } else if (applies && lower.startsWith("disallow:")) {
                String path = line.substring(9).trim();
                if (!path.isEmpty()) disallowed.add(path);
            } else if (applies && lower.startsWith("allow:")) {
                String path = line.substring(6).trim();
                if (!path.isEmpty()) disallowed.remove(path);
            }
        }

        return disallowed;
    }

    /**
     * Attempts to fetch the <code>robots.txt</code> file content from the given URL.
     *
     * @param u the base URL whose <code>robots.txt</code> is to be fetched
     * @return the content of the <code>robots.txt</code> file, or null if not found
     */
    private String fetchRobotsTxt(URL u) {
        try {
            String robotsUrl = u.getProtocol() + "://" + u.getHost() + "/robots.txt";
            Connection.Response resp = Jsoup.connect(robotsUrl)
                .ignoreHttpErrors(true)
                .timeout(CrawlerConfig.getHttpTimeout())
                .userAgent(DEFAULT_AGENT)
                .execute();
            if (resp.statusCode() == 200) return resp.body();
        } catch (IOException ignored) {}
        return null;
    }
}
