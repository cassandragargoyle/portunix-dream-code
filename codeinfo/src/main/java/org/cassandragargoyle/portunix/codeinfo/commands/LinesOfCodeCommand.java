/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeinfo.commands;

import org.cassandragargoyle.portunix.codeinfo.analyzers.JavaLocAnalyzer;
import org.cassandragargoyle.portunix.codeinfo.analyzers.LocResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.MalformedInputException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Command to count lines of code in Java source files
 */
@Command(
    name = "loc",
    description = {
        "Count lines of code in a Java source file.",
        "Code lines include lines with code and a trailing comment.",
        "Empty lines and comment-only lines are not counted as code."
    },
    mixinStandardHelpOptions = true,
    exitCodeListHeading = "%nExit codes:%n",
    exitCodeList = {
        "0:Success",
        "1:Unexpected error during analysis",
        "2:Invalid input (missing file, not a .java file, not UTF-8, bad option)"
    },
    footerHeading = "%nExamples:%n",
    footer = {
        "  ptx-codeinfo loc Example.java",
        "  ptx-codeinfo loc --verbose src/main/java/Example.java"
    }
)
public class LinesOfCodeCommand implements Callable<Integer> {

    static final int EXIT_OK = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_INVALID_INPUT = 2;

    @Spec
    private CommandSpec spec;

    private final JavaLocAnalyzer analyzer;

    @Parameters(
        index = "0",
        description = "Java source file to analyze"
    )
    private File sourceFile;

    @Option(
        names = {"-v", "--verbose"},
        description = "Show detailed breakdown"
    )
    private boolean verbose;

    public LinesOfCodeCommand() {
        this(new JavaLocAnalyzer());
    }

    /**
     * Create the command with a custom analyzer, used by tests
     */
    LinesOfCodeCommand(JavaLocAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public Integer call() {
        // Use picocli writers so output can be redirected in tests
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        try {
            // Validate file exists
            if (!sourceFile.exists()) {
                error(err, "File not found: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate path is a regular file
            if (!sourceFile.isFile()) {
                error(err, "Not a regular file: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate file is readable
            if (!sourceFile.canRead()) {
                error(err, "File is not readable: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate file is a Java file, case insensitive for Windows file names
            if (!sourceFile.getName().toLowerCase(Locale.ROOT).endsWith(".java")) {
                error(err, "File must be a Java source file (.java): " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Analyze file
            LocResult result = analyzer.analyze(sourceFile);

            if (result.hasParseErrors()) {
                warning(err, sourceFile.getName()
                        + " has syntax errors, counts are based on lexical analysis only");
            }

            // Output results
            if (verbose) {
                out.println("Analyzed: " + sourceFile.getName());
                out.println("Total lines: " + result.getTotalLines());
                out.println("Code lines: " + result.getCodeLines());
                out.println("Comment lines: " + result.getCommentLines());
                out.println("Empty lines: " + result.getEmptyLines());
            } else {
                out.println(result.getCodeLines() + " lines of code in " + sourceFile.getName());
            }

            return EXIT_OK;

        } catch (MalformedInputException e) {
            error(err, "File is not valid UTF-8 text: " + sourceFile.getPath());
            return EXIT_INVALID_INPUT;
        } catch (Exception e) {
            error(err, "Analysis failed for " + sourceFile.getPath() + ": " + e.getMessage());
            if (verbose) {
                e.printStackTrace(err);
            }
            return EXIT_ERROR;
        }
    }

    /**
     * Print an error message, the prefix is colored when the terminal supports it
     */
    private static void error(PrintWriter err, String message) {
        err.println(Ansi.AUTO.string("@|bold,red Error:|@ ") + message);
    }

    /**
     * Print a warning message, the prefix is colored when the terminal supports it
     */
    private static void warning(PrintWriter err, String message) {
        err.println(Ansi.AUTO.string("@|bold,yellow Warning:|@ ") + message);
    }
}
