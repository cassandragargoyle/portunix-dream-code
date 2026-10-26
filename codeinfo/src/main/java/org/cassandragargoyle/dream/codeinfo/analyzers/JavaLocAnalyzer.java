/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Analyzer for counting lines of code in Java source files
 *
 * Counting rules:
 * - Count: Lines with executable code
 * - Count: Lines with both code and comments (e.g., int x = 1; // comment)
 * - Skip: Empty lines (whitespace only)
 * - Skip: Comment-only lines (single-line // or multi-line)
 *
 * Comments are taken from the lexer, so files with syntax errors are still classified
 */
public class JavaLocAnalyzer {

    private final JavaParser parser;

    public JavaLocAnalyzer() {
        // RAW skips language level validators: they reject modern syntax such as text blocks
        // and walk the AST via reflection, which fails in GraalVM Native Image.
        // Only comment ranges are needed, so the token list and comment attribution
        // are turned off to lower memory use on large files
        this.parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(LanguageLevel.RAW)
            .setStoreTokens(false)
            .setAttributeComments(false));
    }

    /**
     * Analyze a Java source file and count lines of code
     *
     * @param sourceFile Java source file to analyze
     * @return LocResult containing line counts
     * @throws IOException if file cannot be read
     */
    public LocResult analyze(File sourceFile) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);

        // Mark columns covered by comments, one mask per line
        boolean[][] commentMask = new boolean[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            commentMask[i] = new boolean[lines.get(i).length()];
        }

        ParseResult<CompilationUnit> parseResult = parser.parse(sourceFile.toPath());
        parseResult.getCommentsCollection()
            .map(CommentsCollection::getComments)
            .ifPresent(comments -> comments.forEach(comment -> markComment(comment, lines, commentMask)));

        int emptyLines = 0;
        int codeLines = 0;
        int commentLines = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                emptyLines++;
            } else if (hasCodeOutsideComments(line, commentMask[i])) {
                // Code-only and mixed code+comment lines
                codeLines++;
            } else {
                commentLines++;
            }
        }

        return new LocResult(lines.size(), codeLines, commentLines, emptyLines, !parseResult.isSuccessful());
    }

    /**
     * Mark all columns covered by a comment
     */
    private void markComment(Comment comment, List<String> lines, boolean[][] commentMask) {
        if (comment.getRange().isEmpty()) {
            return;
        }
        Range range = comment.getRange().get();
        Position begin = range.begin;
        Position end = range.end;

        // Positions are 1-based and inclusive
        for (int line = begin.line; line <= end.line && line <= lines.size(); line++) {
            boolean[] mask = commentMask[line - 1];
            int from = line == begin.line ? begin.column - 1 : 0;
            int to = line == end.line ? Math.min(end.column, mask.length) : mask.length;
            for (int col = Math.max(from, 0); col < to; col++) {
                mask[col] = true;
            }
        }
    }

    /**
     * Check if a line contains a non-whitespace character outside of comments
     */
    private boolean hasCodeOutsideComments(String line, boolean[] mask) {
        for (int col = 0; col < line.length(); col++) {
            if (!mask[col] && !Character.isWhitespace(line.charAt(col))) {
                return true;
            }
        }
        return false;
    }
}
