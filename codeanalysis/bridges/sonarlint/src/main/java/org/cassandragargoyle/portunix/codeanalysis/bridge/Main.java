/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.bridge;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "ptx-sonarlint-bridge",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "SonarLint bridge for ptx-codeanalysis",
    subcommands = {
        AnalyzeCommand.class,
        RulesCommand.class
    }
)
public class Main implements Runnable {

    @Option(names = {"--help-ai"}, help = true, description = "Show machine-readable help in JSON format")
    boolean helpAI;

    public static void main(String[] args) {
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        try {
            cmd.parseArgs(args);
            if (main.helpAI) {
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
              "tool": "ptx-sonarlint-bridge",
              "version": "%s",
              "description": "SonarLint bridge for ptx-codeanalysis",
              "commands": [
                {
                  "name": "analyze",
                  "brief": "Run SonarLint analysis on specified files",
                  "description": "Reads JSON from stdin with files, languages, and rules config. Returns JSON with issues and stats."
                },
                {
                  "name": "rules",
                  "brief": "List available SonarLint rules",
                  "parameters": [
                    {"name": "--language", "type": "string", "required": false, "description": "Filter by language"}
                  ]
                }
              ]
            }""".formatted(version);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
