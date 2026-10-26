/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaLocAnalyzer
 */
class JavaLocAnalyzerTest {

    private Path tempDir;

    private JavaLocAnalyzer analyzer;

    @BeforeEach
    void setUp() throws Exception {
        analyzer = new JavaLocAnalyzer();
        // JUnit 5.3 has no @TempDir support
        tempDir = Files.createTempDirectory("codeinfo-test");
    }

    @AfterEach
    void deleteTempDir() throws Exception {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void testSimpleClass() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("SimpleClass.java"));

        assertCounts(result, 15, 10, 1, 4);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testEmptyFile() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("EmptyFile.java"));

        assertCounts(result, 1, 0, 0, 1);
    }

    @Test
    void testCommentOnlyFile() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("CommentOnlyFile.java"));

        assertCounts(result, 9, 0, 7, 2);
    }

    @Test
    void testMixedContent() throws Exception {
        // Javadoc, inline line and block comments, code after a multi-line comment
        LocResult result = analyzer.analyze(getFixtureFile("MixedContent.java"));

        assertCounts(result, 20, 10, 6, 4);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testMultipleCommentsOnOneLine() throws Exception {
        // Two block comments on one line are comment-only, code between comments is code
        LocResult result = analyzer.analyze(getFixtureFile("MultipleComments.java"));

        assertCounts(result, 12, 6, 5, 1);
    }

    @Test
    void testCommentMarkersInStringLiteralsAreCode() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("StringLiterals.java"));

        assertCounts(result, 11, 9, 1, 1);
        assertFalse(result.hasParseErrors(), "Text blocks must parse with Java 21 language level");
    }

    @Test
    void testUnicodeContent() throws Exception {
        // Diacritics and surrogate pairs must not shift comment columns
        LocResult result = analyzer.analyze(getFixtureFile("UnicodeContent.java"));

        assertCounts(result, 8, 5, 2, 1);
    }

    @Test
    void testSyntaxErrorStillClassifiesComments() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("SyntaxError.java"));

        assertCounts(result, 9, 5, 3, 1);
        assertTrue(result.hasParseErrors());
    }

    @Test
    void testModernJavaSyntaxParses() throws Exception {
        File file = writeTempFile("Modern.java", """
            record Point(int x, int y) {
                Object describe(Object o) {
                    return switch (o) {
                        case Integer i when i > 0 -> i; // pattern with guard
                        default -> o;
                    };
                }
            }
            """);

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 8, 8, 0, 0);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testZeroByteFile() throws Exception {
        File file = writeTempFile("Zero.java", "");

        assertCounts(analyzer.analyze(file), 0, 0, 0, 0);
    }

    @Test
    void testWindowsLineEndings() throws Exception {
        File file = writeTempFile("Crlf.java", "class A {\r\n// comment\r\n\r\n  int x; /* c */\r\n}\r\n");

        assertCounts(analyzer.analyze(file), 5, 3, 1, 1);
    }

    @Test
    void testTabIndentation() throws Exception {
        File file = writeTempFile("Tabs.java", "class A {\n\t\t/* comment */\n\t\tint x; // c\n}\n");

        assertCounts(analyzer.analyze(file), 4, 3, 1, 0);
    }

    @Test
    void testInvalidUtf8Throws() throws Exception {
        Path path = tempDir.resolve("Binary.java");
        Files.write(path, new byte[] {(byte) 0xff, (byte) 0xfe, 'c', 'l', 'a', 's', 's'});

        assertThrows(MalformedInputException.class, () -> analyzer.analyze(path.toFile()));
    }

    @Test
    void testCountsAlwaysSumToTotal() throws Exception {
        for (String name : new String[] {"SimpleClass.java", "MixedContent.java", "MultipleComments.java",
                "StringLiterals.java", "UnicodeContent.java", "SyntaxError.java"}) {
            LocResult result = analyzer.analyze(getFixtureFile(name));
            assertEquals(result.getTotalLines(),
                    result.getCodeLines() + result.getCommentLines() + result.getEmptyLines(), name);
        }
    }

    private static void assertCounts(LocResult result, int total, int code, int comment, int empty) {
        assertAll(result.toString(),
            () -> assertEquals(total, result.getTotalLines(), "total"),
            () -> assertEquals(code, result.getCodeLines(), "code"),
            () -> assertEquals(comment, result.getCommentLines(), "comment"),
            () -> assertEquals(empty, result.getEmptyLines(), "empty"));
    }

    private File writeTempFile(String name, String content) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path.toFile();
    }

    /**
     * Helper method to get fixture file from test resources
     */
    private File getFixtureFile(String filename) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("fixtures/" + filename);
        assertNotNull(resource, "Fixture file not found: " + filename);
        // URI conversion decodes spaces and non-ASCII characters in the path
        return new File(resource.toURI());
    }
}
