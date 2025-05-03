package com.eulerity.hackathon.imagefinder.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load crawler settings from a configuration file with support for 
 * environment variable overrides. Provides access to key crawling parameters.
 * <p>
 * Values are loaded from {@code config.properties} and can be overridden by 
 * environment variables using uppercase and underscore notation (e.g., CRAWLER_THREAD_COUNT).
 * </p>
 */
public final class CrawlerConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = CrawlerConfig.class.getClassLoader()
                                                 .getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                throw new RuntimeException("config.properties not found on classpath");
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Retrieves a configuration value by key.
     * Checks environment variable first, then properties file, then uses default.
     *
     * @param key the configuration key (e.g., "crawler.thread.count")
     * @param defaultVal the fallback value if no environment or property value is found
     * @return the resolved configuration value
     */
    private static String get(String key, String defaultVal) {
        String envName = key.toUpperCase().replace('.', '_');
        String env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return props.getProperty(key, defaultVal);
    }

    /**
     * @return the number of threads to use for concurrent crawling
     */
    public static int getThreadCount() {
        return Integer.parseInt(get("crawler.thread.count", "8"));
    }

    /**
     * @return the delay (in milliseconds) between requests to the same domain
     */
    public static int getPolitenessDelay() {
        return Integer.parseInt(get("crawler.politeness.delay", "200"));
    }

    /**
     * @return the maximum number of pages to crawl
     */
    public static int getMaxPages() {
        return Integer.parseInt(get("crawler.max.pages", "100"));
    }

    /**
     * @return the HTTP timeout value (in milliseconds) for each request
     */
    public static int getHttpTimeout() {
        return Integer.parseInt(get("crawler.http.timeout", "10000"));
    }

    /**
     * @return the User-Agent string to send with HTTP requests
     */
    public static String getUserAgent() {
        return get("crawler.user.agent", "ImageFinderBot/1.0");
    }

    /**
     * @return the maximum recursion depth for sub-page crawling
     */
    public static int getMaxDepth() {
        return Integer.parseInt(get("crawler.max.depth", "3"));
    }

    // Prevent instantiation
    private CrawlerConfig() { }
}
