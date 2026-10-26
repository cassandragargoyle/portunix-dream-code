/*
 *  SPDX-License-Identifier: MIT
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
    void testUnicodeEscapedCommentIsComment() throws Exception {
        // javac decodes unicode escapes first, so this line is a line comment
        File file = writeTempFile("Escaped.java", "class U {\n    \\u002F\\u002F hidden comment\n    int a;\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 4, 3, 1, 0);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testUnicodeEscapesKeepCommentColumns() throws Exception {
        // Escapes are longer than the decoded characters, comment ranges must still match
        File file = writeTempFile("Columns.java", """
            class U {
                int \\u0061 = 1; /* c */ int b;
                /* \\u0041\\u0042\\u0043 */
                char c = '\\u0041'; // tail
            }
            """);

        assertCounts(analyzer.analyze(file), 5, 4, 1, 0);
    }

    @Test
    void testEscapedNewlineEndsLineComment() throws Exception {
        // An escaped line feed ends the line comment for javac, so the rest of the line is code
        File file = writeTempFile("Newline.java", "class E {\n    // note \\u000A int x;\n}\n");

        assertCounts(analyzer.analyze(file), 3, 3, 0, 0);
    }

    @Test
    void testUnclosedBlockCommentRunsToEndOfFile() throws Exception {
        File file = writeTempFile("Unclosed.java", """
            class X { /* ok */
                int a; // c
                /* unterminated
                int b;
            }
            """);

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 5, 2, 3, 0);
        assertTrue(result.hasParseErrors());
    }

    @Test
    void testUnclosedTextBlockStaysCode() throws Exception {
        // An unclosed text block is not a comment, its lines stay code
        File file = writeTempFile("TextBlock.java", "class T {\n    String s = \"\"\"\n        /* not a comment\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 4, 4, 0, 0);
        assertTrue(result.hasParseErrors());
    }

    @Test
    void testEscapedWhitespaceLineIsEmpty() throws Exception {
        File file = writeTempFile("EscapedSpace.java", "class A {\n    \\u0020\\u0009\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 3, 2, 0, 1);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testEscapedLineFeedAfterCommentIsComment() throws Exception {
        File file = writeTempFile("EscapedFeed.java", "class A {\n    // c \\u000A\n    int a;\n}\n");

        assertCounts(analyzer.analyze(file), 4, 3, 1, 0);
    }

    @Test
    void testEscapedBackslashDoesNotStartEscape() throws Exception {
        // Two backslashes are an escaped backslash followed by plain text
        File file = writeTempFile("EscapedBackslash.java", "class A {\n    String s = \"\\\\u00G1\";\n    \\\\u0020\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 4, 4, 0, 0);
    }

    @Test
    void testInvalidEscapeIsSyntaxError() throws Exception {
        // javac rejects a unicode escape without four hex digits, also inside comments
        File file = writeTempFile("InvalidEscape.java", "class A {\n    // bad \\u00G1 escape\n    int a;\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 4, 3, 1, 0);
        assertTrue(result.hasParseErrors());
    }

    @Test
    void testTruncatedEscapeIsSyntaxError() throws Exception {
        File file = writeTempFile("TruncatedEscape.java", "class A {} // end \\u00");

        assertTrue(analyzer.analyze(file).hasParseErrors());
    }

    @Test
    void testValidEscapesAreNotSyntaxErrors() throws Exception {
        File file = writeTempFile("ValidEscapes.java", "class A {\n    char c = '\\uuu0041'; // \\u00e9\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 3, 3, 0, 0);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testOnlyJavaWhitespaceMakesEmptyLine() throws Exception {
        // JLS 3.6: space, tab and form feed; U+2028, U+3000 and vertical tab are not whitespace
        String content = "class A {\n"
            + "    \t" + (char) 0x0C + "\n"
            + "    " + (char) 0x2028 + "\n"
            + "    " + (char) 0x3000 + "\n"
            + "    " + (char) 0x0B + "\n"
            + "}\n";
        File file = writeTempFile("JavaWhitespace.java", content);

        assertCounts(analyzer.analyze(file), 6, 5, 0, 1);
    }

    @Test
    void testEscapedNonJavaWhitespaceIsCode() throws Exception {
        File file = writeTempFile("EscapedSeparator.java", "class A {\n    \\u2028\n    \\u3000\n}\n");

        assertCounts(analyzer.analyze(file), 4, 4, 0, 0);
    }

    @Test
    void testUnicodeSpacesInLiteralsAndCommentsAreValid() throws Exception {
        LocResult result = analyzer.analyze(getFixtureFile("UnicodeSpaces.java"));

        assertCounts(result, 12, 8, 4, 0);
        assertFalse(result.hasParseErrors());
    }

    @Test
    void testUnicodeSpaceInCodeIsSyntaxError() throws Exception {
        // javac rejects U+3000 outside comments and literals as an illegal character
        File file = writeTempFile("IllegalSpace.java", "class A {\n    int a;" + IDEOGRAPHIC_SPACE + "\n}\n");

        LocResult result = analyzer.analyze(file);

        assertCounts(result, 3, 3, 0, 0);
        assertTrue(result.hasParseErrors());
    }

    @Test
    void testEscapedUnicodeSpaceInCodeIsSyntaxError() throws Exception {
        File file = writeTempFile("EscapedIllegalSpace.java", "class A {\n    int a;\\u2028\n}\n");

        assertTrue(analyzer.analyze(file).hasParseErrors());
    }

    @Test
    void testEscapedUnicodeSpaceInStringIsValid() throws Exception {
        File file = writeTempFile("EscapedSpaceString.java", "class A { String s = \"\\u3000\"; }\n");

        assertFalse(analyzer.analyze(file).hasParseErrors());
    }

    @Test
    void testUnicodeSpaceOutsideLiteralIsSyntaxErrorNextToValidOne() throws Exception {
        File file = writeTempFile("MixedSpaces.java",
            "class A {\n    String s = \"" + IDEOGRAPHIC_SPACE + "\";" + IDEOGRAPHIC_SPACE + "\n}\n");

        assertTrue(analyzer.analyze(file).hasParseErrors());
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

    private static final char IDEOGRAPHIC_SPACE = (char) 0x3000;

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
