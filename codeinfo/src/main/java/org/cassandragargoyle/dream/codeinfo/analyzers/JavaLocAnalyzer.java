/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Analyzer for counting lines of code in Java source files
 *
 * Counting rules:
 * - Count: Lines with executable code
 * - Count: Lines with both code and comments (e.g., int x = 1; // comment)
 * - Skip: Empty lines (whitespace only)
 * - Skip: Comment-only lines (single-line // or multi-line)
 */
public class JavaLocAnalyzer {

    private final JavaParser parser;

    public JavaLocAnalyzer() {
        this.parser = new JavaParser();
    }

    /**
     * Analyze a Java source file and count lines of code
     *
     * @param sourceFile Java source file to analyze
     * @return LocResult containing line counts
     * @throws IOException if file cannot be read
     */
    public LocResult analyze(File sourceFile) throws IOException {
        // Read all lines from file
        List<String> lines = Files.readAllLines(sourceFile.toPath());
        int totalLines = lines.size();

        // Parse the file to get comments
        ParseResult<CompilationUnit> parseResult = parser.parse(sourceFile);

        // Get comment lines
        Set<Integer> commentOnlyLines = new HashSet<>();
        Set<Integer> mixedLines = new HashSet<>();

        if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
            CompilationUnit cu = parseResult.getResult().get();

            // Get all comments
            List<Comment> comments = cu.getAllContainedComments();
            comments.addAll(cu.getOrphanComments());

            for (Comment comment : comments) {
                if (comment.getRange().isPresent()) {
                    int beginLine = comment.getRange().get().begin.line;
                    int endLine = comment.getRange().get().end.line;

                    for (int i = beginLine; i <= endLine; i++) {
                        if (i <= lines.size()) {
                            String line = lines.get(i - 1).trim();

                            // Check if line has code before or after comment
                            boolean hasCode = lineHasCode(line, comment, i, beginLine, endLine);

                            if (hasCode) {
                                mixedLines.add(i);
                            } else {
                                commentOnlyLines.add(i);
                            }
                        }
                    }
                }
            }
        }

        // Count different line types
        int emptyLines = 0;
        int codeLines = 0;
        int commentLines = 0;

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).trim();

            if (line.isEmpty()) {
                emptyLines++;
            } else if (mixedLines.contains(lineNumber)) {
                // Line has both code and comment - count as code
                codeLines++;
            } else if (commentOnlyLines.contains(lineNumber)) {
                // Comment-only line
                commentLines++;
            } else {
                // Regular code line
                codeLines++;
            }
        }

        return new LocResult(totalLines, codeLines, commentLines, emptyLines);
    }

    /**
     * Check if a line contains code in addition to comments
     */
    private boolean lineHasCode(String line, Comment comment, int lineNumber, int commentBegin, int commentEnd) {
        // For single-line comments
        if (lineNumber == commentBegin && lineNumber == commentEnd) {
            // Check if there's code before or after the comment
            if (line.startsWith("//")) {
                return false; // Pure comment line
            }

            // Check for code before single-line comment
            int commentStart = line.indexOf("//");
            if (commentStart > 0) {
                String beforeComment = line.substring(0, commentStart).trim();
                if (!beforeComment.isEmpty() && !beforeComment.equals("/*") && !beforeComment.equals("*")) {
                    return true; // Has code before comment
                }
            }
        }

        // For multi-line comments
        if (lineNumber == commentBegin) {
            // First line of multi-line comment
            int commentStart = line.indexOf("/*");
            if (commentStart > 0) {
                String beforeComment = line.substring(0, commentStart).trim();
                if (!beforeComment.isEmpty()) {
                    return true; // Has code before comment
                }
            }
            // Check if comment ends on same line
            int commentEndPos = line.indexOf("*/");
            if (commentEndPos >= 0 && commentEndPos + 2 < line.length()) {
                String afterComment = line.substring(commentEndPos + 2).trim();
                if (!afterComment.isEmpty()) {
                    return true; // Has code after comment
                }
            }
        } else if (lineNumber == commentEnd) {
            // Last line of multi-line comment
            int commentEnd2 = line.indexOf("*/");
            if (commentEnd2 >= 0 && commentEnd2 + 2 < line.length()) {
                String afterComment = line.substring(commentEnd2 + 2).trim();
                if (!afterComment.isEmpty()) {
                    return true; // Has code after comment
                }
            }
        } else if (lineNumber > commentBegin && lineNumber < commentEnd) {
            // Middle line of multi-line comment - pure comment
            return false;
        }

        return false;
    }
}
