package com.eulerity.hackathon.imagefinder.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Utility methods for working with URLs, including normalization,
 * domain checks, and disallowed path detection.
 */
public class UrlUtil {

    /**
     * Normalizes the given URL string by resolving ".", "..", and other URI components.
     *
     * @param urlStr the URL string to normalize
     * @return a normalized URL string
     * @throws MalformedURLException if the URL syntax is invalid
     */
    public static String normalize(String urlStr) throws MalformedURLException {
        try {
            URL url = new URL(urlStr);
            return url.toURI().normalize().toString();
        } catch (URISyntaxException e) {
            throw new MalformedURLException("Invalid URL syntax: " + urlStr);
        }
    }

    /**
     * Converts a string into a {@link URL} object.
     *
     * @param urlStr the URL string
     * @return a URL object
     * @throws MalformedURLException if the string is not a valid URL
     */
    public static URL toUrl(String urlStr) throws MalformedURLException {
        return new URL(urlStr);
    }

    /**
     * Returns the path portion of a URL.
     *
     * @param url the URL
     * @return the path component, or "/" if none
     */
    public static String getPath(URL url) {
        String path = url.getPath();
        return (path != null) ? path : "/";
    }

    /**
     * Checks if two URLs belong to the same domain (host).
     *
     * @param url  the first URL
     * @param base the second URL to compare against
     * @return true if both URLs have the same host, false otherwise
     */
    public static boolean isSameDomain(URL url, URL base) {
        return url.getHost().equalsIgnoreCase(base.getHost());
    }

    /**
     * Determines whether the path of the given URL is disallowed
     * based on a set of disallowed path prefixes.
     *
     * @param url        the URL to check
     * @param disallowed the set of disallowed path prefixes
     * @return true if the URL path starts with any disallowed prefix
     */
    public static boolean isDisallowed(URL url, Set<String> disallowed) {
        String path = getPath(url);
        for (String prefix : disallowed) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
