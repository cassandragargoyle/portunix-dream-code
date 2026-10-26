/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.Providers;
import com.github.javaparser.Range;
import com.github.javaparser.UnicodeEscapeProcessingProvider;
import com.github.javaparser.UnicodeEscapeProcessingProvider.PositionMapping;
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
 * Comments are taken from the lexer, so files with syntax errors are still classified.
 * Unicode escapes are decoded before lexing, as in javac, and an unclosed block
 * comment runs to the end of the file
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
        String source = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
        List<String> lines = source.lines().toList();

        // Mark columns covered by comments, one mask per line
        boolean[][] commentMask = new boolean[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            commentMask[i] = new boolean[lines.get(i).length()];
        }

        CommentScan scan = scanComments(source);
        List<Range> commentRanges = scan.commentRanges();
        if (!scan.successful()) {
            // The lexer stops at an unclosed block comment and drops it. Closing it at
            // the end of the file brings it back with its real start position
            CommentScan closed = scanComments(source + "*/");
            if (closed.blockComments() > scan.blockComments()) {
                commentRanges = closed.commentRanges();
            }
        }
        commentRanges.forEach(range -> markComment(range, lines, commentMask));

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

        return new LocResult(lines.size(), codeLines, commentLines, emptyLines, !scan.successful());
    }

    /**
     * Parse the source and return comment ranges in positions of the original text
     */
    private CommentScan scanComments(String source) {
        UnicodeEscapeProcessingProvider provider = new UnicodeEscapeProcessingProvider(Providers.provider(source));
        ParseResult<CompilationUnit> result = parser.parse(ParseStart.COMPILATION_UNIT, provider);

        // Ranges refer to the decoded text, map them back when the source has unicode escapes
        PositionMapping mapping = provider.getPositionMapping();
        List<Comment> comments = result.getCommentsCollection()
            .map(CommentsCollection::getComments)
            .map(List::<Comment>copyOf)
            .orElse(List.of());
        List<Range> ranges = comments.stream()
            .flatMap(comment -> comment.getRange().stream())
            .map(range -> mapping.isEmpty() ? range : mapping.transform(range))
            .toList();
        long blockComments = comments.stream().filter(comment -> !comment.isLineComment()).count();

        return new CommentScan(ranges, blockComments, result.isSuccessful());
    }

    /**
     * Comments found by one parser run
     */
    private record CommentScan(List<Range> commentRanges, long blockComments, boolean successful) {
    }

    /**
     * Mark all columns covered by a comment
     */
    private void markComment(Range range, List<String> lines, boolean[][] commentMask) {
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
