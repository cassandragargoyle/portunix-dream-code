/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.commands;

import org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI;
import org.cassandragargoyle.dream.codeinfo.analyzers.JavaLocAnalyzer;
import org.cassandragargoyle.dream.codeinfo.analyzers.LocResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the loc command executed through the root CLI
 */
class LinesOfCodeCommandTest {

    private Path tempDir;

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @BeforeEach
    void createTempDir() throws Exception {
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
    void testSimpleOutput() throws Exception {
        int exitCode = run("loc", fixturePath("SimpleClass.java"));

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertEquals("10 lines of code in SimpleClass.java", out.toString().strip());
        assertEquals("", err.toString());
    }

    @Test
    void testVerboseOutput() throws Exception {
        int exitCode = run("loc", "--verbose", fixturePath("MixedContent.java"));

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertEquals(String.join(System.lineSeparator(),
                "Analyzed: MixedContent.java",
                "Total lines: 20",
                "Code lines: 10",
                "Comment lines: 6",
                "Empty lines: 4"), out.toString().strip());
    }

    @Test
    void testShortVerboseOption() throws Exception {
        int exitCode = run("loc", "-v", fixturePath("SimpleClass.java"));

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertTrue(out.toString().contains("Total lines: 15"));
    }

    @Test
    void testSyntaxErrorWarnsButSucceeds() throws Exception {
        int exitCode = run("loc", fixturePath("SyntaxError.java"));

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertEquals("5 lines of code in SyntaxError.java", out.toString().strip());
        assertTrue(err.toString().startsWith("Warning: SyntaxError.java has syntax errors"));
    }

    @Test
    void testFileNotFound() {
        int exitCode = run("loc", tempDir.resolve("Missing.java").toString());

        assertEquals(LinesOfCodeCommand.EXIT_INVALID_INPUT, exitCode);
        assertTrue(err.toString().startsWith("Error: File not found:"));
        assertEquals("", out.toString());
    }

    @Test
    void testDirectoryRejected() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("Folder.java"));

        int exitCode = run("loc", dir.toString());

        assertEquals(LinesOfCodeCommand.EXIT_INVALID_INPUT, exitCode);
        assertTrue(err.toString().startsWith("Error: Not a regular file:"));
    }

    @Test
    void testNonJavaFileRejected() throws Exception {
        Path file = Files.writeString(tempDir.resolve("notes.txt"), "text");

        int exitCode = run("loc", file.toString());

        assertEquals(LinesOfCodeCommand.EXIT_INVALID_INPUT, exitCode);
        assertTrue(err.toString().startsWith("Error: File must be a Java source file (.java):"));
    }

    @Test
    void testUppercaseJavaExtensionAccepted() throws Exception {
        Path file = Files.writeString(tempDir.resolve("Upper.JAVA"), "class Upper {}\n");

        int exitCode = run("loc", file.toString());

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertEquals("1 lines of code in Upper.JAVA", out.toString().strip());
    }

    @Test
    void testUnreadableFileRejected() throws Exception {
        Path file = Files.writeString(tempDir.resolve("Locked.java"), "class A {}");
        File locked = file.toFile();
        // Not enforceable on all platforms or when running as root
        assumeTrue(locked.setReadable(false) && !locked.canRead());

        try {
            int exitCode = run("loc", file.toString());

            assertEquals(LinesOfCodeCommand.EXIT_INVALID_INPUT, exitCode);
            assertTrue(err.toString().startsWith("Error: File is not readable:"));
        } finally {
            locked.setReadable(true);
        }
    }

    @Test
    void testInvalidUtf8Rejected() throws Exception {
        Path file = tempDir.resolve("Binary.java");
        Files.write(file, new byte[] {(byte) 0xff, (byte) 0xfe, 'c', 'l', 'a', 's', 's'});

        int exitCode = run("loc", file.toString());

        assertEquals(LinesOfCodeCommand.EXIT_INVALID_INPUT, exitCode);
        assertTrue(err.toString().startsWith("Error: File is not valid UTF-8 text:"));
    }

    @Test
    void testMissingFileParameter() {
        int exitCode = run("loc");

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(err.toString().contains("Missing required parameter"));
    }

    @Test
    void testUnknownOption() throws Exception {
        int exitCode = run("loc", "--unknown", fixturePath("SimpleClass.java"));

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(err.toString().contains("Unknown option"));
    }

    @Test
    void testCommandHelp() {
        int exitCode = run("loc", "--help");

        assertEquals(LinesOfCodeCommand.EXIT_OK, exitCode);
        assertTrue(out.toString().contains("Usage: ptx-codeinfo loc"));
    }

    @Test
    void testCommandHelpShowsExamplesAndExitCodes() {
        run("loc", "--help");

        String help = out.toString();
        assertTrue(help.contains("Examples:"));
        assertTrue(help.contains("ptx-codeinfo loc --verbose"));
        assertTrue(help.contains("Exit codes:"));
        assertTrue(help.contains("Invalid input"));
    }

    @Test
    void testAnalysisFailureReturnsError() throws Exception {
        int exitCode = runWithAnalyzer(new FailingAnalyzer(), fixturePath("SimpleClass.java"));

        assertEquals(LinesOfCodeCommand.EXIT_ERROR, exitCode);
        assertEquals("", out.toString());
        assertTrue(err.toString().startsWith("Error: Analysis failed for "));
        assertTrue(err.toString().contains("disk read failed"));
        assertFalse(err.toString().contains("at org."), "Stack trace only in verbose mode");
    }

    @Test
    void testAnalysisFailurePrintsStackTraceInVerboseMode() throws Exception {
        int exitCode = runWithAnalyzer(new FailingAnalyzer(), "--verbose", fixturePath("SimpleClass.java"));

        assertEquals(LinesOfCodeCommand.EXIT_ERROR, exitCode);
        assertTrue(err.toString().contains("java.io.IOException: disk read failed"));
    }

    @Test
    void testMessagesHaveNoAnsiCodesWithoutTerminal() {
        // Tests run without a console, so colors must be disabled automatically
        run("loc", tempDir.resolve("Missing.java").toString());

        assertFalse(err.toString().contains("\u001B["));
    }

    private int run(String... args) {
        CommandLine cmd = new CommandLine(new PtxCodeInfoCLI());
        cmd.setOut(new PrintWriter(out, true));
        cmd.setErr(new PrintWriter(err, true));
        return cmd.execute(args);
    }

    private int runWithAnalyzer(JavaLocAnalyzer analyzer, String... args) {
        CommandLine cmd = new CommandLine(new LinesOfCodeCommand(analyzer));
        cmd.setOut(new PrintWriter(out, true));
        cmd.setErr(new PrintWriter(err, true));
        return cmd.execute(args);
    }

    /**
     * Analyzer that always fails, simulates an unexpected I/O error
     */
    private static class FailingAnalyzer extends JavaLocAnalyzer {
        @Override
        public LocResult analyze(File sourceFile) throws IOException {
            throw new IOException("disk read failed");
        }
    }

    private String fixturePath(String filename) throws Exception {
        URL resource = getClass().getClassLoader().getResource("fixtures/" + filename);
        assertNotNull(resource, "Fixture file not found: " + filename);
        return new File(resource.toURI()).getPath();
    }
}
