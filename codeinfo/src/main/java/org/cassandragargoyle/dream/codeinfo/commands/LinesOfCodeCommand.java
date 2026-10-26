/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.commands;

import org.cassandragargoyle.dream.codeinfo.analyzers.JavaLocAnalyzer;
import org.cassandragargoyle.dream.codeinfo.analyzers.LocResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
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
        try {
            // Validate file exists
            if (!sourceFile.exists()) {
                System.err.println("Error: File not found: " + sourceFile.getPath());
                return 2;
            }

            // Validate file is readable
            if (!sourceFile.canRead()) {
                System.err.println("Error: File is not readable: " + sourceFile.getPath());
                return 2;
            }

            // Validate file is a Java file
            if (!sourceFile.getName().endsWith(".java")) {
                System.err.println("Error: File must be a Java source file (.java): " + sourceFile.getPath());
                return 2;
            }

            // Analyze file
            JavaLocAnalyzer analyzer = new JavaLocAnalyzer();
            LocResult result = analyzer.analyze(sourceFile);

            // Output results
            if (verbose) {
                System.out.println("Analyzed: " + sourceFile.getName());
                System.out.println("Total lines: " + result.getTotalLines());
                System.out.println("Code lines: " + result.getCodeLines());
                System.out.println("Comment lines: " + result.getCommentLines());
                System.out.println("Empty lines: " + result.getEmptyLines());
            } else {
                System.out.println(result.getCodeLines() + " lines of code in " + sourceFile.getName());
            }

            return 0;

        } catch (Exception e) {
            System.err.println("Error analyzing file: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(System.err);
            }
            return 1;
        }
    }
}
