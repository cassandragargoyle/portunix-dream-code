/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PtxCodeInfoCLI entry point and --help-ai flag
 */
class PtxCodeInfoCLITest {

    @Test
    void testHelpAIFlagIsParsed() {
        PtxCodeInfoCLI app = new PtxCodeInfoCLI();
        new CommandLine(app).parseArgs("--help-ai");
        assertTrue(app.helpAI);
    }

    @Test
    void testHelpAIFlagSkipsRequiredSubcommandParameters() {
        // Missing <sourceFile> for loc must not prevent --help-ai
        PtxCodeInfoCLI app = new PtxCodeInfoCLI();
        assertDoesNotThrow(() -> new CommandLine(app).parseArgs("--help-ai", "loc"));
        assertTrue(app.helpAI);
    }

    @Test
    void testHelpAIFlagDefaultsToFalse() {
        PtxCodeInfoCLI app = new PtxCodeInfoCLI();
        new CommandLine(app).parseArgs();
        assertFalse(app.helpAI);
    }

    @Test
    void testHelpAIJsonContainsRequiredFields() {
        String version = new CommandLine(new PtxCodeInfoCLI()).getCommandSpec().version()[0];
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
        CommandLine cmd = new CommandLine(new PtxCodeInfoCLI());
        String json = helpAIJsonCompact(cmd.getCommandSpec().version()[0]);

        cmd.getSubcommands().keySet().stream()
            .filter(name -> !name.equals("help"))
            .forEach(name -> assertTrue(json.contains("\"name\":\"" + name + "\""),
                "Subcommand missing in --help-ai output: " + name));
    }

    @Test
    void testRunHelpAIPrintsJson() {
        StringWriter out = new StringWriter();
        int exitCode = PtxCodeInfoCLI.run(new String[] {"--help-ai"}, writer(out), writer(new StringWriter()));

        assertEquals(0, exitCode);
        assertTrue(out.toString().strip().startsWith("{"));
        assertTrue(out.toString().contains("\"tool\": \"ptx-codeinfo\""));
    }

    @Test
    void testRunWithoutCommandShowsUsage() {
        StringWriter out = new StringWriter();
        int exitCode = PtxCodeInfoCLI.run(new String[0], writer(out), writer(new StringWriter()));

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("Usage: ptx-codeinfo"));
        assertTrue(out.toString().contains("loc"));
    }

    @Test
    void testRunReportsInvalidOption() {
        StringWriter err = new StringWriter();
        int exitCode = PtxCodeInfoCLI.run(new String[] {"--unknown"}, writer(new StringWriter()), writer(err));

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(err.toString().contains("Unknown option"));
    }

    private static PrintWriter writer(StringWriter target) {
        return new PrintWriter(target, true);
    }

    private static String helpAIJsonCompact(String version) {
        // Strip formatting whitespace outside of string values
        return PtxCodeInfoCLI.helpAIJson(version).trim().replaceAll("\\s*([{}\\[\\]:,])\\s*", "$1");
    }
}
