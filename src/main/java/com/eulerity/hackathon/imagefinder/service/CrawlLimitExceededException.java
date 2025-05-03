package com.eulerity.hackathon.imagefinder.service;

/**
 * Exception indicating that the web crawler has exceeded one of the configured crawl limits,
 * such as maximum pages or depth.
 */
public class CrawlLimitExceededException extends Exception {

    /**
     * Constructs a new CrawlLimitExceededException with the specified detail message.
     *
     * @param message the detail message explaining the exceeded limit
     */
    public CrawlLimitExceededException(String message) {
        super(message);
    }
}
