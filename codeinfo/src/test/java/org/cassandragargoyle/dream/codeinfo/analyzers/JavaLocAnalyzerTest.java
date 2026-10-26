/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaLocAnalyzer
 */
class JavaLocAnalyzerTest {

    private JavaLocAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JavaLocAnalyzer();
    }

    @Test
    void testSimpleClass() throws Exception {
        File file = getFixtureFile("SimpleClass.java");
        LocResult result = analyzer.analyze(file);

        // SimpleClass.java has:
        // - package declaration (code)
        // - class declaration (code)
        // - private field (code)
        // - constructor with 2 lines (code)
        // - getter method with inline comment (code)
        // - closing braces (code)
        // - 1 comment line
        // - 4-5 empty lines
        // Total: 15 lines (may vary slightly based on file ending)

        assertTrue(result.getTotalLines() >= 14 && result.getTotalLines() <= 16,
                   "Expected 14-16 total lines, got " + result.getTotalLines());
        assertTrue(result.getCodeLines() >= 8, "Expected at least 8 code lines, got " + result.getCodeLines());
        assertTrue(result.getCommentLines() >= 1, "Expected at least 1 comment line, got " + result.getCommentLines());
        assertTrue(result.getEmptyLines() >= 3, "Expected at least 3 empty lines, got " + result.getEmptyLines());
    }

    @Test
    void testEmptyFile() throws Exception {
        File file = getFixtureFile("EmptyFile.java");
        LocResult result = analyzer.analyze(file);

        assertEquals(1, result.getTotalLines());
        assertEquals(1, result.getEmptyLines());
        assertEquals(0, result.getCodeLines());
    }

    @Test
    void testCommentOnlyFile() throws Exception {
        File file = getFixtureFile("CommentOnlyFile.java");
        LocResult result = analyzer.analyze(file);

        // All lines should be either comments or empty
        assertEquals(result.getTotalLines(), result.getCommentLines() + result.getEmptyLines());
        assertEquals(0, result.getCodeLines());
    }

    @Test
    void testMixedContent() throws Exception {
        File file = getFixtureFile("MixedContent.java");
        LocResult result = analyzer.analyze(file);

        // Mixed content should have code lines (including mixed code+comment lines)
        assertTrue(result.getCodeLines() > 0, "Expected code lines in mixed content");
        assertTrue(result.getCommentLines() > 0, "Expected comment lines in mixed content");

        // Total should equal sum of all line types
        assertEquals(result.getTotalLines(),
                result.getCodeLines() + result.getCommentLines() + result.getEmptyLines());
    }

    @Test
    void testResultNotNull() throws Exception {
        File file = getFixtureFile("SimpleClass.java");
        LocResult result = analyzer.analyze(file);

        assertNotNull(result);
        assertTrue(result.getTotalLines() >= 0);
        assertTrue(result.getCodeLines() >= 0);
        assertTrue(result.getCommentLines() >= 0);
        assertTrue(result.getEmptyLines() >= 0);
    }

    /**
     * Helper method to get fixture file from test resources
     */
    private File getFixtureFile(String filename) {
        URL resource = getClass().getClassLoader().getResource("fixtures/" + filename);
        assertNotNull(resource, "Fixture file not found: " + filename);
        return new File(resource.getFile());
    }
}
