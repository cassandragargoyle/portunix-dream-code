/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeinfo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consistency checks between plugin.json and the CLI and build artifacts
 */
class PluginManifestTest {

    private static String manifest;

    @BeforeAll
    static void loadManifest() throws IOException {
        // Tests run in the module directory, next to plugin.json
        manifest = Files.readString(Path.of("plugin.json")).replaceAll("\\s+", "");
    }

    @Test
    void testManifestDeclaresNativeBinary() {
        assertTrue(manifest.contains("\"type\":\"helper\""));
        assertTrue(manifest.contains("\"binary\":\"ptx-codeinfo\",\"runtime\":\"native\""));
    }

    @Test
    void testManifestFallbackMatchesJarName() {
        // The JAR name is set by <finalName> in pom.xml
        assertTrue(manifest.contains("\"fallback\":{\"binary\":\"ptx-codeinfo.jar\",\"runtime\":\"java\""));
    }

    @Test
    void testManifestListsAllSubcommands() {
        CommandLine cmd = new CommandLine(new Main());

        cmd.getSubcommands().keySet().stream()
            .filter(name -> !name.equals("help"))
            .forEach(name -> assertTrue(manifest.matches(".*\"subcommands\":\\[[^\\]]*\"" + name + "\".*"),
                "Subcommand missing in plugin.json: " + name));
    }
}
