# Issue #11: ptx-codeinfo: unterminated string literal hides later comments in LOC count

## Summary

When a Java file has an unterminated string literal, the JavaParser lexer stops with a
lexical error in the middle of the file. All comments after that point are lost, so later
comment-only lines are counted as code.

**Status**: 📋 Open
**Type**: bug
**Priority**: low
**Component**: codeinfo
**Created**: 2026-10-26
**GitHub**: #11
**Related**: #2

## Problem Description

`JavaLocAnalyzer` takes comment ranges from the JavaParser lexer. A lexical error stops
the lexer, and the comments collection then holds only the comments before the error.

Example:

```java
class A {
    String s = "abc;
    // c
    int b;
}
```

Current result: 5 code lines, 0 comment lines. Line 3 is a comment-only line and
should be a comment line.

`javac` rejects such files ("unclosed string literal"), so the impact is limited to files
that do not compile. A syntax error warning is already printed.

The same mechanism was fixed for an unclosed block comment at the end of the file in #2
(the source is parsed again with `*/` appended). That approach does not work here,
because the lexer stops in the middle of the file.

Found during acceptance of #2: observation O2 in `docs/testing/acceptance-002.md`.

## Possible Approaches

1. **Restart after the bad line**: on a lexical error that is not at the end of the
   file, take the line of the error, keep the comments found before it, and lex the
   rest of the file again (string literals cannot span lines, so the next line is a
   safe restart point). Repeat for further errors
2. **Own minimal lexer for the error path**: a small scanner for comments, string and
   character literals and text blocks, used only when JavaParser reports a lexical
   error. More code, but independent of JavaParser error messages

Approach 1 reuses JavaParser and is likely smaller. Position mapping for unicode
escapes must keep working in both approaches.

## Acceptance Criteria

1. In the example above, line 3 is a comment line and lines 1, 2, 4 and 5 are code
2. Several unterminated string literals in one file are handled
3. The syntax error warning is still printed
4. Files without lexical errors give the same counts as before (no second parser run)
5. Unclosed block comment and unclosed text block behave as before (#2)

## Testing

- Unit tests in `JavaLocAnalyzerTest` for the example, for two unterminated strings,
  for an unterminated character literal, and for an unterminated string followed by an
  unclosed block comment
- Regression: all Java files in the repository and the acceptance inputs of #2 give the
  same counts as before, except the files with unterminated literals
- `make test-codeinfo-native`: native binary matches the JAR
