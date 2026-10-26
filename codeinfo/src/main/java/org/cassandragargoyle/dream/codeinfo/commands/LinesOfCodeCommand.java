/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.commands;

import org.cassandragargoyle.dream.codeinfo.analyzers.JavaLocAnalyzer;
import org.cassandragargoyle.dream.codeinfo.analyzers.LocResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.MalformedInputException;
import java.util.concurrent.Callable;

/**
 * Command to count lines of code in Java source files
 */
@Command(
    name = "loc",
    description = "Count lines of code in a Java source file",
    mixinStandardHelpOptions = true
)
public class LinesOfCodeCommand implements Callable<Integer> {

    static final int EXIT_OK = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_INVALID_INPUT = 2;

    @Spec
    private CommandSpec spec;

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

    @Override
    public Integer call() {
        // Use picocli writers so output can be redirected in tests
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        try {
            // Validate file exists
            if (!sourceFile.exists()) {
                err.println("Error: File not found: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate path is a regular file
            if (!sourceFile.isFile()) {
                err.println("Error: Not a regular file: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate file is readable
            if (!sourceFile.canRead()) {
                err.println("Error: File is not readable: " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Validate file is a Java file
            if (!sourceFile.getName().endsWith(".java")) {
                err.println("Error: File must be a Java source file (.java): " + sourceFile.getPath());
                return EXIT_INVALID_INPUT;
            }

            // Analyze file
            JavaLocAnalyzer analyzer = new JavaLocAnalyzer();
            LocResult result = analyzer.analyze(sourceFile);

            if (result.hasParseErrors()) {
                err.println("Warning: " + sourceFile.getName()
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
            err.println("Error: File is not valid UTF-8 text: " + sourceFile.getPath());
            return EXIT_INVALID_INPUT;
        } catch (Exception e) {
            err.println("Error analyzing file: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(err);
            }
            return EXIT_ERROR;
        }
    }
}
