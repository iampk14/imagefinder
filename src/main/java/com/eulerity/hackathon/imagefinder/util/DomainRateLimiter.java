package com.eulerity.hackathon.imagefinder.util;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces a minimum delay between successive requests to the same domain.
 * Useful for preventing overloading or getting banned during crawling.
 */
public class DomainRateLimiter {

    private final long politenessMillis;
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    /**
     * Constructs a DomainRateLimiter with the specified delay between requests.
     *
     * @param politenessMillis minimum delay in milliseconds between two requests to the same domain
     */
    public DomainRateLimiter(long politenessMillis) {
        this.politenessMillis = politenessMillis;
    }

    /**
     * Blocks the current thread if the time since the last request to the domain is less than the configured delay.
     *
     * @param url the URL whose host will be checked for rate-limiting
     */
    public void acquire(URL url) {
        String host = url.getHost().toLowerCase();
        synchronized (host.intern()) {
            long now = System.currentTimeMillis();
            Long last = lastAccess.get(host);
            if (last != null) {
                long wait = politenessMillis - (now - last);
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            lastAccess.put(host, System.currentTimeMillis());
        }
    }
}
