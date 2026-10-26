/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeinfo.analyzers;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.Providers;
import com.github.javaparser.Range;
import com.github.javaparser.UnicodeEscapeProcessingProvider;
import com.github.javaparser.UnicodeEscapeProcessingProvider.PositionMapping;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Unicode escapes are decoded before lexing, as in javac: escaped whitespace is
 * whitespace and an invalid escape is a syntax error. Unicode spaces that are not Java
 * whitespace (e.g. U+3000) are a syntax error outside comments and literals. An unclosed
 * block comment runs to the end of the file
 */
public class JavaLocAnalyzer {

    private final JavaParser parser;
    private final JavaParser literalParser;

    public JavaLocAnalyzer() {
        // RAW skips language level validators: they reject modern syntax such as text blocks
        // and walk the AST via reflection, which fails in GraalVM Native Image.
        // Only comment ranges are needed, so the token list and comment attribution
        // are turned off to lower memory use on large files
        this.parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(LanguageLevel.RAW)
            .setStoreTokens(false)
            .setAttributeComments(false));
        // AST nodes get ranges only with stored tokens; used for rare files with unicode spaces
        this.literalParser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(LanguageLevel.RAW)
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
        if (scan.lexerReachedEnd()) {
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
        boolean invalidEscape = false;
        List<Position> illegalSpaces = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Escapes need a backslash, other lines skip the escape scan
            boolean[] whitespaceMask = null;
            boolean[] illegalSpaceMask = null;
            if (line.indexOf('\\') >= 0) {
                whitespaceMask = new boolean[line.length()];
                illegalSpaceMask = new boolean[line.length()];
                invalidEscape |= !markEscapedWhitespace(line, whitespaceMask, illegalSpaceMask);
            }
            collectIllegalSpaces(line, i + 1, commentMask[i], illegalSpaceMask, illegalSpaces);

            if (isBlank(line, whitespaceMask)) {
                emptyLines++;
            } else if (hasCodeOutsideComments(line, commentMask[i], whitespaceMask)) {
                // Code-only and mixed code+comment lines
                codeLines++;
            } else {
                commentLines++;
            }
        }

        // With a failed parse the warning is printed anyway, literal ranges are only needed otherwise
        boolean illegalSpace = scan.successful() && !illegalSpaces.isEmpty()
                && !insideLiterals(source, illegalSpaces);

        return new LocResult(lines.size(), codeLines, commentLines, emptyLines,
                !scan.successful() || invalidEscape || illegalSpace);
    }

    /**
     * Check if all positions are inside string, character or text block literals
     */
    private boolean insideLiterals(String source, List<Position> positions) {
        UnicodeEscapeProcessingProvider provider = new UnicodeEscapeProcessingProvider(Providers.provider(source));
        ParseResult<CompilationUnit> result = literalParser.parse(ParseStart.COMPILATION_UNIT, provider);

        PositionMapping mapping = provider.getPositionMapping();
        List<Range> literals = result.getResult()
            .map(unit -> unit.findAll(LiteralStringValueExpr.class))
            .orElse(List.of())
            .stream()
            .flatMap(literal -> literal.getRange().stream())
            .map(range -> mapping.isEmpty() ? range : mapping.transform(range))
            .toList();
        return positions.stream().allMatch(position -> literals.stream().anyMatch(range -> range.contains(position)));
    }

    /**
     * Collect positions outside comments of unicode spaces that are not Java whitespace
     */
    private static void collectIllegalSpaces(String line, int lineNumber, boolean[] commentMask,
                                             boolean[] illegalSpaceMask, List<Position> positions) {
        for (int col = 0; col < line.length(); col++) {
            boolean illegal = isIllegalSpace(line.charAt(col)) || (illegalSpaceMask != null && illegalSpaceMask[col]);
            if (illegal && !commentMask[col]) {
                positions.add(new Position(lineNumber, col + 1));
            }
        }
    }

    /**
     * Check for a character that Character.isWhitespace accepts but the Java language does not
     */
    private static boolean isIllegalSpace(char c) {
        return Character.isWhitespace(c) && !isJavaWhitespace(c);
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
        boolean lexerReachedEnd = result.getProblems().stream()
            .map(Problem::getMessage)
            .anyMatch(message -> message.startsWith("Lexical error") && message.contains("<EOF>"));

        return new CommentScan(ranges, blockComments, result.isSuccessful(), lexerReachedEnd);
    }

    /**
     * Comments found by one parser run
     *
     * lexerReachedEnd is true when the lexer failed at the end of the file, which happens
     * for an unclosed block comment or text block
     */
    private record CommentScan(List<Range> commentRanges, long blockComments, boolean successful,
                               boolean lexerReachedEnd) {
    }

    /**
     * Mark unicode escapes that decode to whitespace, and escapes that decode to unicode
     * spaces which are not Java whitespace
     *
     * @return false if the line has an invalid unicode escape
     */
    private static boolean markEscapedWhitespace(String line, boolean[] whitespaceMask, boolean[] illegalSpaceMask) {
        boolean valid = true;
        int i = 0;
        while (i < line.length()) {
            if (line.charAt(i) != '\\') {
                i++;
                continue;
            }
            int backslashes = i;
            while (i < line.length() && line.charAt(i) == '\\') {
                i++;
            }
            // Only a backslash after an even number of backslashes starts an escape
            if ((i - backslashes) % 2 == 0 || i == line.length() || line.charAt(i) != 'u') {
                continue;
            }
            int escapeStart = i - 1;
            while (i < line.length() && line.charAt(i) == 'u') {
                i++;
            }
            if (!isHexDigits(line, i, 4)) {
                valid = false;
                continue;
            }
            char decoded = (char) Integer.parseInt(line, i, i + 4, 16);
            i += 4;
            if (isJavaWhitespace(decoded)) {
                Arrays.fill(whitespaceMask, escapeStart, i, true);
            } else if (isIllegalSpace(decoded)) {
                illegalSpaceMask[escapeStart] = true;
            }
        }
        return valid;
    }

    /**
     * Check if count characters from start are ASCII hex digits
     */
    private static boolean isHexDigits(String line, int start, int count) {
        if (start + count > line.length()) {
            return false;
        }
        for (int i = start; i < start + count; i++) {
            char c = line.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
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
     * Check if a column is whitespace, written directly or as part of a unicode escape
     */
    private static boolean isWhitespace(String line, int col, boolean[] whitespaceMask) {
        return isJavaWhitespace(line.charAt(col)) || (whitespaceMask != null && whitespaceMask[col]);
    }

    /**
     * Check for whitespace as defined by the Java language (JLS 3.6): space, tab, form feed
     * and line terminators. Character.isWhitespace also accepts characters such as U+2028
     * or U+3000, which javac rejects as illegal characters
     */
    private static boolean isJavaWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\f' || c == '\n' || c == '\r';
    }

    /**
     * Check if a line has only whitespace, written directly or as unicode escapes
     */
    private boolean isBlank(String line, boolean[] whitespaceMask) {
        for (int col = 0; col < line.length(); col++) {
            if (!isWhitespace(line, col, whitespaceMask)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a line contains a non-whitespace character outside of comments
     */
    private boolean hasCodeOutsideComments(String line, boolean[] commentMask, boolean[] whitespaceMask) {
        for (int col = 0; col < line.length(); col++) {
            if (!commentMask[col] && !isWhitespace(line, col, whitespaceMask)) {
                return true;
            }
        }
        return false;
    }
}
