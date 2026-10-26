/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo;

import org.cassandragargoyle.dream.codeinfo.commands.LinesOfCodeCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Main entry point for ptx-codeinfo CLI application
 * Fast code analysis tool powered by GraalVM Native Image
 */
@Command(
    name = "ptx-codeinfo",
    version = "1.0.2",
    description = "Fast code analysis tool powered by GraalVM",
    subcommands = {
        LinesOfCodeCommand.class,
        CommandLine.HelpCommand.class
    },
    mixinStandardHelpOptions = true
)
public class PtxCodeInfoCLI implements Callable<Integer> {

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "Display version information")
    boolean versionRequested;

    @Option(names = {"--help-ai"}, help = true, description = "Show machine-readable help in JSON format")
    boolean helpAI;

    public static void main(String[] args) {
        PtxCodeInfoCLI app = new PtxCodeInfoCLI();
        CommandLine cmd = new CommandLine(app);
        try {
            cmd.parseArgs(args);
            if (app.helpAI) {
                System.out.println(helpAIJson(cmd.getCommandSpec().version()[0]));
                System.exit(0);
            }
        } catch (CommandLine.ParameterException e) {
            // Let execute() report invalid arguments in the standard way
        }

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    /**
     * Machine-readable description of the CLI for AI assistants
     */
    static String helpAIJson(String version) {
        return """
            {
              "tool": "ptx-codeinfo",
              "version": "%s",
              "description": "Fast code analysis tool powered by GraalVM",
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
            }""".formatted(version);
    }

    @Override
    public Integer call() {
        // Show help when no command specified
        CommandLine.usage(this, System.out);
        return 0;
    }
}
