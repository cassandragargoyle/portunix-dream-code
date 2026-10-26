/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
	name = "ptx-codescan-bridge",
	mixinStandardHelpOptions = true,
	version = "1.0.0",
	description = "CodeScan bridge for code validation - annotations and patterns",
	subcommands =
	{
		AnalyzeCommand.class
	}
)
public class Main implements Runnable
{

	@Option(names = {"--help-ai"}, help = true, description = "Show machine-readable help in JSON format")
	boolean helpAI;

	public static void main(String[] args)
	{
		Main main = new Main();
		CommandLine cmd = new CommandLine(main);
		try
		{
			cmd.parseArgs(args);
			if (main.helpAI)
			{
				System.out.println(helpAIJson(cmd.getCommandSpec().version()[0]));
				System.exit(0);
			}
		}
		catch (CommandLine.ParameterException e)
		{
			// Let execute() report invalid arguments in the standard way
		}

		int exitCode = cmd.execute(args);
		System.exit(exitCode);
	}

	/**
	 * Machine-readable description of the CLI for AI assistants
	 */
	static String helpAIJson(String version)
	{
		return """
			{
			  "tool": "ptx-codescan-bridge",
			  "version": "%s",
			  "description": "CodeScan bridge for code validation - annotations and patterns",
			  "commands": [
			    {
			      "name": "analyze",
			      "brief": "Run CodeScan analysis on specified files",
			      "description": "Reads JSON from stdin with files, annotations, patterns, and severity mapping. Returns JSON with issues and stats."
			    }
			  ]
			}""".formatted(version);
	}

	@Override
	public void run()
	{
		CommandLine.usage(this, System.out);
	}
}
