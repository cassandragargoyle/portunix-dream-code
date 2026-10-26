/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.dream.codeinfo.analyzers;

/**
 * Result of lines of code analysis
 */
public class LocResult {
    private final int totalLines;
    private final int codeLines;
    private final int commentLines;
    private final int emptyLines;
    private final boolean parseErrors;

    public LocResult(int totalLines, int codeLines, int commentLines, int emptyLines, boolean parseErrors) {
        this.totalLines = totalLines;
        this.codeLines = codeLines;
        this.commentLines = commentLines;
        this.emptyLines = emptyLines;
        this.parseErrors = parseErrors;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public int getCodeLines() {
        return codeLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public int getEmptyLines() {
        return emptyLines;
    }

    /**
     * True if the source has syntax errors and counts rely on lexical analysis only
     */
    public boolean hasParseErrors() {
        return parseErrors;
    }

    @Override
    public String toString() {
        return "LocResult{" +
                "totalLines=" + totalLines +
                ", codeLines=" + codeLines +
                ", commentLines=" + commentLines +
                ", emptyLines=" + emptyLines +
                ", parseErrors=" + parseErrors +
                '}';
    }
}
