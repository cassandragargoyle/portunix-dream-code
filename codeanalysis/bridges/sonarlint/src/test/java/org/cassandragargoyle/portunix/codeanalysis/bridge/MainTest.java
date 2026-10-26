/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testHelpAIFlagIsParsed() {
        Main main = new Main();
        new CommandLine(main).parseArgs("--help-ai");
        assertTrue(main.helpAI);
    }

    @Test
    void testHelpAIJsonIsValid() throws Exception {
        String version = new CommandLine(new Main()).getCommandSpec().version()[0];
        JsonNode root = new ObjectMapper().readTree(Main.helpAIJson(version));

        assertEquals("ptx-sonarlint-bridge", root.get("tool").asText());
        assertEquals(version, root.get("version").asText());
        assertTrue(root.has("description"));
        assertTrue(root.get("commands").isArray());
        assertEquals("analyze", root.get("commands").get(0).get("name").asText());
        assertEquals("rules", root.get("commands").get(1).get("name").asText());
    }

    @Test
    void testHelpAIJsonMatchesSubcommands() throws Exception {
        CommandLine cmd = new CommandLine(new Main());
        JsonNode root = new ObjectMapper().readTree(Main.helpAIJson(cmd.getCommandSpec().version()[0]));

        Set<String> documented = new HashSet<>();
        root.get("commands").forEach(c -> documented.add(c.get("name").asText()));
        assertEquals(cmd.getSubcommands().keySet(), documented);
    }
}
