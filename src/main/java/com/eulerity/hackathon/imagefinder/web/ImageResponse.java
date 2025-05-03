package com.eulerity.hackathon.imagefinder.web;

import java.util.List;

/**
 * Response wrapper for image search results. Serialized to JSON as:
 * <pre>
 * {
 *   "images": [ { "url": "...", "likelyLogo": true }, ... ],
 *   "error": null
 * }
 * </pre>
 * or in case of an error:
 * <pre>
 * {
 *   "images": [],
 *   "error": "error message"
 * }
 * </pre>
 */
public class ImageResponse {

    /**
     * Represents a single image result with its URL and logo likelihood.
     */
    public static class Item {
        /** The absolute URL of the image. */
        public final String url;

        /** Whether the image is likely a logo, based on filename heuristics. */
        public final boolean likelyLogo;

        /**
         * Constructs an image item.
         *
         * @param url the image URL
         * @param likelyLogo true if likely a logo
         */
        public Item(String url, boolean likelyLogo) {
            this.url = url;
            this.likelyLogo = likelyLogo;
        }
    }

    /** List of image items returned from the crawler. */
    public final List<Item> images;

    /** Error message if the crawl failed; null otherwise. */
    public final String error;

    /**
     * Constructs an image response with a list of images and optional error message.
     *
     * @param images the list of image results
     * @param error the error message, or null if no error occurred
     */
    public ImageResponse(List<Item> images, String error) {
        this.images = images;
        this.error = error;
    }
}
