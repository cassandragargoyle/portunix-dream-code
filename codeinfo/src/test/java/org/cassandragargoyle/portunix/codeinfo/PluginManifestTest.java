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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consistency checks between plugin.json and the CLI and build artifacts
 */
class PluginManifestTest {

    private static String rawManifest;
    private static String manifest;

    @BeforeAll
    static void loadManifest() throws IOException {
        // Tests run in the module directory, next to plugin.json
        rawManifest = Files.readString(Path.of("plugin.json"));
        manifest = rawManifest.replaceAll("\\s+", "");
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
    void testManifestListsAllCommands() {
        CommandLine cmd = new CommandLine(new Main());
        List<String> manifestNames = commandEntries().stream().map(PluginManifestTest::commandName).toList();

        Main.commandNames(cmd)
            .forEach(name -> assertTrue(manifestNames.contains(name), "Command missing in plugin.json: " + name));
    }

    @Test
    void testManifestListsOnlyRealCommands() {
        CommandLine cmd = new CommandLine(new Main());
        List<String> cliNames = Main.commandNames(cmd);

        commandEntries().stream()
            .map(PluginManifestTest::commandName)
            .forEach(name -> assertTrue(cliNames.contains(name), "Command not provided by the CLI: " + name));
    }

    @Test
    void testManifestHasNoPlaceholderCommand() {
        // A command entry named after the plugin or the binary is a placeholder, not a command
        commandEntries().stream()
            .map(PluginManifestTest::commandName)
            .forEach(name -> {
                assertNotEquals("code-info", name);
                assertNotEquals("ptx-codeinfo", name);
            });
    }

    @Test
    void testCommandDescriptionsMatchCli() {
        CommandLine cmd = new CommandLine(new Main());

        for (String entry : commandEntries()) {
            String name = commandName(entry);
            String description = topLevelField(entry, "description");
            assertNotNull(description, "Command without description in plugin.json: " + name);
            CommandLine subcommand = cmd.getSubcommands().get(name);
            assertNotNull(subcommand, "Command not provided by the CLI: " + name);

            // The manifest uses the first line of the CLI description, without the ending period
            String cliDescription = subcommand.getCommandSpec().usageMessage().description()[0];
            assertEquals(cliDescription.replaceAll("\\.$", ""), stringValue(description));
        }
    }

    @Test
    void testCommandExamplesStartWithCommand() {
        for (String entry : commandEntries()) {
            String prefix = "portunix code-info " + commandName(entry);
            String examples = topLevelField(entry, "examples");
            if (examples == null) {
                continue;
            }
            for (String example : arrayElements(examples)) {
                String text = stringValue(example);
                assertTrue(text.equals(prefix) || text.startsWith(prefix + " "),
                    "Example does not start with '" + prefix + "': " + text);
            }
        }
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

    // Minimal JSON reader for the manifest, the module has no JSON library and the tests
    // must not depend on key order or on nested objects such as parameters

    /**
     * Raw JSON objects of the top-level commands array
     */
    private static List<String> commandEntries() {
        String commands = topLevelField(rawManifest, "commands");
        assertNotNull(commands, "No commands array in plugin.json");
        return arrayElements(commands);
    }

    private static String commandName(String entry) {
        String name = topLevelField(entry, "name");
        assertNotNull(name, "Command without name in plugin.json: " + entry);
        return stringValue(name);
    }

    /**
     * Raw value of a top-level key of a JSON object, or null when the key is missing
     */
    private static String topLevelField(String object, String key) {
        int depth = 0;
        int i = 0;
        while (i < object.length()) {
            char c = object.charAt(i);
            if (c == '"') {
                int end = stringEnd(object, i);
                int colon = skipWhitespace(object, end + 1);
                boolean isKey = colon < object.length() && object.charAt(colon) == ':';
                if (depth == 1 && isKey && object.substring(i + 1, end).equals(key)) {
                    int start = skipWhitespace(object, colon + 1);
                    return object.substring(start, valueEnd(object, start)).strip();
                }
                i = end + 1;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
            i++;
        }
        return null;
    }

    /**
     * Raw values of a JSON array
     */
    private static List<String> arrayElements(String array) {
        assertTrue(array.startsWith("["), "Not a JSON array in plugin.json: " + array);
        List<String> elements = new ArrayList<>();
        int i = skipWhitespace(array, 1);
        while (i < array.length() && array.charAt(i) != ']') {
            int end = valueEnd(array, i);
            elements.add(array.substring(i, end).strip());
            i = end < array.length() && array.charAt(end) == ',' ? skipWhitespace(array, end + 1) : end;
        }
        return elements;
    }

    /**
     * Content of a raw JSON string value
     */
    private static String stringValue(String raw) {
        assertTrue(raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\""),
            "Not a JSON string in plugin.json: " + raw);
        return raw.substring(1, raw.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /**
     * End index (exclusive) of the value that starts at the given index
     */
    private static int valueEnd(String json, int start) {
        int depth = 0;
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                i = stringEnd(json, i) + 1;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * Index of the closing quote of the string that starts at the given index
     */
    private static int stringEnd(String json, int start) {
        int i = start + 1;
        while (json.charAt(i) != '"') {
            i += json.charAt(i) == '\\' ? 2 : 1;
        }
        return i;
    }

    private static int skipWhitespace(String json, int start) {
        int i = start;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }
}
