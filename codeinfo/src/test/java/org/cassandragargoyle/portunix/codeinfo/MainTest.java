/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeinfo;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Main entry point and --help-ai flag
 */
class MainTest {

    @Test
    void testHelpAIFlagIsParsed() {
        Main app = new Main();
        new CommandLine(app).parseArgs("--help-ai");
        assertTrue(app.helpAI);
    }

    @Test
    void testHelpAIFlagSkipsRequiredSubcommandParameters() {
        // Missing <sourceFile> for loc must not prevent --help-ai
        Main app = new Main();
        assertDoesNotThrow(() -> new CommandLine(app).parseArgs("--help-ai", "loc"));
        assertTrue(app.helpAI);
    }

    @Test
    void testHelpAIFlagDefaultsToFalse() {
        Main app = new Main();
        new CommandLine(app).parseArgs();
        assertFalse(app.helpAI);
    }

    @Test
    void testHelpAIJsonContainsRequiredFields() {
        String version = new CommandLine(new Main()).getCommandSpec().version()[0];
        String json = helpAIJsonCompact(version);

        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"tool\":\"ptx-codeinfo\""));
        assertTrue(json.contains("\"version\":\"" + version + "\""));
        assertTrue(json.contains("\"description\":"));
        assertTrue(json.contains("\"commands\":["));
        assertTrue(json.contains("\"name\":\"loc\""));
    }

    @Test
    void testHelpAIJsonListsAllSubcommands() {
        CommandLine cmd = new CommandLine(new Main());
        String json = helpAIJsonCompact(cmd.getCommandSpec().version()[0]);

        cmd.getSubcommands().keySet().stream()
            .filter(name -> !name.equals("help"))
            .forEach(name -> assertTrue(json.contains("\"name\":\"" + name + "\""),
                "Subcommand missing in --help-ai output: " + name));
    }

    @Test
    void testRunHelpAIPrintsJson() {
        StringWriter out = new StringWriter();
        int exitCode = Main.run(new String[] {"--help-ai"}, writer(out), writer(new StringWriter()));

        assertEquals(0, exitCode);
        assertTrue(out.toString().strip().startsWith("{"));
        assertTrue(out.toString().contains("\"tool\": \"ptx-codeinfo\""));
    }

    @Test
    void testRunWithoutCommandShowsUsage() {
        StringWriter out = new StringWriter();
        int exitCode = Main.run(new String[0], writer(out), writer(new StringWriter()));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("Usage: ptx-codeinfo"));
        assertTrue(out.toString().contains("loc"));
    }

    @Test
    void testRunReportsInvalidOption() {
        StringWriter err = new StringWriter();
        int exitCode = Main.run(new String[] {"--unknown"}, writer(new StringWriter()), writer(err));

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(err.toString().contains("Unknown option"));
    }

    @Test
    void testRunLocOnTestdataSample() {
        // Expected counts are documented in testdata/README.md
        StringWriter out = new StringWriter();
        int exitCode = Main.run(new String[] {"loc", "--verbose", "testdata/Sample.java"},
            writer(out), writer(new StringWriter()));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("Total lines: 26"));
        assertTrue(out.toString().contains("Code lines: 15"));
        assertTrue(out.toString().contains("Comment lines: 6"));
        assertTrue(out.toString().contains("Empty lines: 5"));
    }

    private static PrintWriter writer(StringWriter target) {
        return new PrintWriter(target, true);
    }

    private static String helpAIJsonCompact(String version) {
        // Strip formatting whitespace outside of string values
        return Main.helpAIJson(version).trim().replaceAll("\\s*([{}\\[\\]:,])\\s*", "$1");
    }
}
