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

        Main.commandNames(cmd)
            .forEach(name -> assertTrue(manifest.matches(".*\"subcommands\":\\[[^\\]]*\"" + name + "\".*"),
                "Subcommand missing in plugin.json: " + name));
    }

    @Test
    void testCliVersionMatchesManifest() {
        // Fails when plugin.json changes but the CLI keeps an old or hard-coded version
        String manifestVersion = Main.parseVersion(manifest);
        CommandLine cmd = new CommandLine(new Main());

        assertEquals(manifestVersion, Main.pluginVersion());
        assertEquals("ptx-codeinfo version " + manifestVersion, cmd.getCommandSpec().version()[0]);
        assertTrue(Main.helpAIJson(Main.pluginVersion()).contains("\"version\": \"" + manifestVersion + "\""));
    }

    @Test
    void testCliDescriptionMatchesManifest() {
        assertTrue(manifest.contains("\"description\":\"" + Main.DESCRIPTION.replaceAll("\\s+", "") + "\""));
    }
}
