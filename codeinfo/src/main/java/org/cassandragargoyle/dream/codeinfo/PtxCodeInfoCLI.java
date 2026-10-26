/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo;

import org.cassandragargoyle.dream.codeinfo.commands.LinesOfCodeCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
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

    @Spec
    private CommandSpec spec;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "Display version information")
    boolean versionRequested;

    @Option(names = {"--help-ai"}, help = true, description = "Show machine-readable help in JSON format")
    boolean helpAI;

    public static void main(String[] args) {
        System.exit(run(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    /**
     * Run the CLI with the given output writers and return the exit code
     */
    static int run(String[] args, PrintWriter out, PrintWriter err) {
        PtxCodeInfoCLI app = new PtxCodeInfoCLI();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(out);
        cmd.setErr(err);
        try {
            cmd.parseArgs(args);
            if (app.helpAI) {
                out.println(helpAIJson(cmd.getCommandSpec().version()[0]));
                return 0;
            }
        } catch (CommandLine.ParameterException e) {
            // Let execute() report invalid arguments in the standard way
        }

        return cmd.execute(args);
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
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }
}
