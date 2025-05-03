package com.eulerity.hackathon.imagefinder.parser;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link RobotsTxtParser} to verify correct parsing of robots.txt content
 * for both wildcard and specific user-agents.
 */
class RobotsTxtParserTest {

    private final String sample = 
        "User-Agent: *\n" +
        "Disallow: /private\n" +
        "# comment line\n" +
        "User-Agent: ImageFinderBot/1.0\n" +
        "Disallow: /bot-only\n" +
        "Allow: /bot-only/allowed\n";

    /**
     * Tests that disallowed paths are correctly parsed for the default user-agent,
     * taking into account allow/deny rules and precedence.
     */
    @Test
    void parsesWildcardAndSpecific() {
        Set<String> rules = RobotsTxtParser.parse(sample, RobotsTxtParser.DEFAULT_AGENT);
        assertTrue(rules.contains("/private"), "Should disallow /private from wildcard rules");
        assertTrue(rules.contains("/bot-only"), "Should disallow /bot-only for specific user-agent");
        assertFalse(rules.contains("/bot-only/allowed"), "Should allow explicitly allowed path");
    }
}
