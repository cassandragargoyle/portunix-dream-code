/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeinfo;

import org.cassandragargoyle.portunix.codeinfo.commands.LinesOfCodeCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point for ptx-codeinfo CLI application
 * Fast code analysis tool powered by GraalVM Native Image
 */
@Command(
    name = "ptx-codeinfo",
    versionProvider = Main.PluginVersionProvider.class,
    description = Main.DESCRIPTION,
    subcommands = {
        LinesOfCodeCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class Main implements Callable<Integer> {

    static final String DESCRIPTION = "Fast code analysis tool powered by GraalVM Native Image";

    // plugin.json is packaged as a classpath resource, it is the single source of the version
    private static final String MANIFEST_RESOURCE = "/plugin.json";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

    @Spec
    private CommandSpec spec;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    boolean helpRequested;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "Display version information")
    boolean versionRequested;

    @Option(names = {"--help-ai"}, help = true, description = "Show machine-readable help in JSON format")
    boolean helpAI;

    @Option(names = {"--list-commands"}, help = true, description = "List available commands")
    boolean listCommands;

    @Option(names = {"--description"}, help = true, description = "Show a one-line tool description")
    boolean showDescription;

    public static void main(String[] args) {
        System.exit(run(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    /**
     * Run the CLI with the given output writers and return the exit code
     */
    static int run(String[] args, PrintWriter out, PrintWriter err) {
        Main app = new Main();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(out);
        cmd.setErr(err);
        try {
            cmd.parseArgs(args);
            // Meta flags win over subcommands, the dispatcher may append them anywhere
            if (app.helpAI) {
                out.println(helpAIJson(pluginVersion()));
                return 0;
            }
            if (app.listCommands) {
                commandNames(cmd).forEach(out::println);
                return 0;
            }
            if (app.showDescription) {
                out.println(DESCRIPTION);
                return 0;
            }
        } catch (CommandLine.ParameterException e) {
            // Let execute() report invalid arguments in the standard way
        }

        return cmd.execute(args);
    }

    /**
     * Names of the user-facing subcommands, without the built-in help command
     */
    static List<String> commandNames(CommandLine cmd) {
        return cmd.getSubcommands().keySet().stream()
            .filter(name -> !name.equals("help"))
            .toList();
    }

    /**
     * Version from the packaged plugin.json
     */
    static String pluginVersion() {
        try (InputStream in = Main.class.getResourceAsStream(MANIFEST_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Plugin manifest not found on classpath: " + MANIFEST_RESOURCE);
            }
            return parseVersion(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read plugin manifest", e);
        }
    }

    /**
     * Extract the top-level version value from plugin.json content
     */
    static String parseVersion(String manifest) {
        Matcher matcher = VERSION_PATTERN.matcher(manifest);
        if (!matcher.find()) {
            throw new IllegalStateException("Version not found in plugin manifest");
        }
        return matcher.group(1);
    }

    /**
     * Machine-readable description of the CLI for AI assistants
     */
    static String helpAIJson(String version) {
        return """
            {
              "tool": "ptx-codeinfo",
              "version": "%s",
              "description": "%s",
              "commands": [
                {
                  "name": "loc",
                  "brief": "Count lines of code in a Java source file",
                  "parameters": [
                    {"name": "file", "type": "string", "required": true, "description": "Java source file to analyze"},
                    {"name": "--verbose", "type": "bool", "required": false, "description": "Show detailed breakdown"}
                  ]
                }
              ]
            }""".formatted(version, DESCRIPTION);
    }

    @Override
    public Integer call() {
        // Show help when no command specified
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    /**
     * Version in the Portunix helper format: ptx-codeinfo version X.Y.Z
     */
    static class PluginVersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"ptx-codeinfo version " + pluginVersion()};
        }
    }
}
