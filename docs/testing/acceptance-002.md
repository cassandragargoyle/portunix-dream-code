# Acceptance Protocol: Issue #002 — ptx-codeinfo Coverage, Memory, Help and Docs

**Issue**: #2 — Implement ptx-codeinfo CLI Application
**Branch**: `feature/002-ptx-codeinfo-polish`
**Tested commit**: `b1793f9`; re-test of fixes F1-F3: `e4d22dd` (see [Re-test](#re-test-after-fixes-f1-f3)); re-test of fixes O1, O3, O4: `9a34a3d` (see [Re-test](#re-test-after-fixes-o1-o3-o4)); re-test of fix N1: `9c9aed4` (see [Re-test](#re-test-after-fix-n1))
**Date**: 2026-10-26
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES

## Scope

Follow-up to #9. The branch adds a JaCoCo coverage gate, makes the CLI entry point
and the `loc` command testable, lowers memory use (parser without token list and
comment attribution, native `-R:MaxNewSize=8m`), adds examples, exit codes and colored
prefixes to the output, and completes the documentation and CHANGELOG.

Issue #2 stays open after this merge (see [Not Tested](#not-tested) and the
remaining tasks in the issue file).

## Plan

- Build with `make build`, run `make test`, `make coverage-codeinfo`
- Build the native image (GraalVM CE 21.0.2), run `make test-codeinfo-native`
- Build a reference JAR from `main` and run every black-box case on three binaries:
  native (branch), JAR (branch), JAR (`main`). A difference to `main` in counts or
  exit code is a regression; changed help text and error wording are expected
- Edge cases and failure injection: encodings, line endings, special files, huge
  input, invalid syntax
- Check test strength with two manual mutations in `JavaLocAnalyzer`
- Verify the performance and memory values documented in `codeinfo/README.md`

## Cases

### Build and automated tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | Given the branch, when `make build`, then it succeeds | PASS |
| TC02 | When `make test`, then JUnit 38/38 and pytest 12/12 pass | PASS |
| TC03 | When `make coverage-codeinfo`, then the 95% line coverage gate is met (97.4%) | PASS |
| TC04 | When `make build-codeinfo-native` and `make test-codeinfo-native`, then all 13 checks match the JAR | PASS |
| TC05 | Given the analyzer ignores the comment mask (mutation), when tests run, then 12 tests fail | PASS |
| TC06 | Given comments always start at column 0 (mutation), when tests run, then 11 tests fail | PASS |

### Line counting (native = JAR = `main` in all cases)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | Given a UTF-8 file with BOM, when `loc -v`, then the BOM line counts as code | PASS |
| TC11 | Given no trailing newline, only `\n`, only `\r\n`, or old Mac `\r` endings, then lines are counted correctly | PASS |
| TC12 | Given one line of 200 000 characters, then it is 1 code line | PASS |
| TC13 | Given `'/'`, `"/* */"` and a text block with `/*` and `//`, then all lines are code | PASS |
| TC14 | Given Javadoc, block and line comments mixed with code, then comment-only lines are comment lines | PASS |
| TC15 | Given `/* a /* b */ int z;`, then the line is code | PASS |
| TC16 | Given tab-only and tab-indented comment lines, then they are empty and comment lines | PASS |
| TC17 | Given a path with spaces, a symlink, or a file named `.java`, then it is analyzed | PASS |
| TC18 | Given a 50 000 line file, then counts are correct (16 665 code lines) | PASS |
| TC19 | Given all 27 Java files in the repository, then counts are identical to `main` | PASS |

### Error handling and exit codes (same as `main`)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Given a missing file or a dangling symlink, then `Error: File not found`, exit 2 | PASS |
| TC21 | Given a directory or a FIFO named `*.java`, then `Error: Not a regular file`, exit 2, no hang | PASS |
| TC22 | Given a file without read permission, then `Error: File is not readable`, exit 2 | PASS |
| TC23 | Given random binary data, then `Error: File is not valid UTF-8 text`, exit 2 | PASS |
| TC24 | Given `Upper.JAVA`, then `Error: File must be a Java source file`, exit 2 | PASS |
| TC25 | Given an unclosed `/*` or a syntax error, then a warning on stderr, exit 0 | PASS |
| TC26 | When `loc` without a file, with two files, or with an unknown option, then usage error, exit 2 | PASS |
| TC27 | Given an analyzer that throws `IOException` (unit test), then `Error: Analysis failed`, exit 1; stack trace only with `--verbose` | PASS |

### Help and output

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC30 | When `loc --help` or `help loc`, then examples and exit codes are shown, exit 0 | PASS |
| TC31 | When `--help-ai`, then the output is valid JSON with command `loc`, exit 0 | PASS |
| TC32 | When `--version` / `-v`, then `1.0.2`, exit 0 | PASS |
| TC33 | Given a terminal, then `Error:` is bold red and `Warning:` bold yellow; stdout has no escape codes | PASS |
| TC34 | Given a terminal and `NO_COLOR=1`, or the JAR with `-Dpicocli.ansi=false`, then there are no colors | PASS |
| TC35 | Given a pipe instead of a terminal, then there are no escape codes | PASS |

## Coverage

| Class | Line | Branch |
| --- | --- | --- |
| `JavaLocAnalyzer` | 38/39 | 26/28 |
| `LinesOfCodeCommand` | 43/43 | 14/14 |
| `LocResult` | 18/18 | - |
| `PtxCodeInfoCLI` | 15/17 (`main()` with `System.exit`) | 2/2 |
| **Total** | **97.4%** (gate 95%) | |

## Performance (Linux x64, GraalVM CE 21.0.2, median of 20 runs)

| Metric | Target | Native | Note |
| --- | --- | --- | --- |
| Startup (`--version`) | < 50 ms | 4.5 ms | PASS |
| 100 LOC | < 100 ms | 8.5 ms | PASS |
| 5000 LOC | < 500 ms | 82 ms | PASS |
| Memory 100 LOC | < 30 MB | 19.7 MB | PASS |
| Memory 5000 LOC | < 30 MB | 35.4 MB | Above target, documented |
| Binary size | < 20 MB | 22.7 MB | Above target, documented |

The machine had a load average of 14-34 during the run, so times are 1.5-3x higher
than in the README (the JAR was slower by the same factor). Memory values match the
README exactly. Memory for 5000 LOC dropped from 110 MB to 35.4 MB compared with `main`.

## Findings

No regressions. All findings below also happen on `main` and do not block the merge.

| ID | Severity | Finding |
| --- | --- | --- |
| F1 | Low, fixed | Comments written with Unicode escapes (`\u002F\u002F text`) are counted as code and produce a syntax error warning; `javac` treats them as comments |
| F2 | Low, fixed | An unclosed `/*` makes all following lines code lines; `javac` treats them as comment (the file does not compile) |
| F3 | Low, fixed | The `.java` check is case sensitive, so `Example.JAVA` is rejected; this may matter on Windows |
| F4 | Low | Memory grows with file size: a 50 000 line file needs 204 MB and 1.3 s; there is no target for this size |
| F5 | Known | Root `--help` and `-h` are rejected, tracked in #6 |

## Re-test After Fixes F1-F3

Fixes: unicode escapes are decoded before comments are detected (F1), a file that
ends in an unclosed block comment is parsed again with `*/` appended (F2), and the
`.java` check is case insensitive (F3).

- `make build`, `make test` (JUnit 44/44, pytest 12/12), `make coverage-codeinfo`
  (98.4%), `make test-codeinfo-native`: PASS
- 50 inputs (repository Java files and all earlier cases) compared with `main`: only
  the 4 inputs with unicode escape comments or unclosed comments differ, as intended
- `--help-ai`, `--version`, `loc --help`: native output matches the JAR

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC40 | Given `\uuuu002F\u002F` (several `u`), then the line is a comment | PASS |
| TC41 | Given `"\\u002F\\u002F"` (escaped backslash), then the line is code | PASS |
| TC42 | Given `/* x *\u002F int a;` (escaped close), then the line is code | PASS |
| TC43 | Given `\u002F* block` over two lines (escaped open), then both lines are comments | PASS |
| TC44 | Given an escape before a multi-line block comment, then columns are mapped and the middle line is a comment | PASS |
| TC45 | Given `// a \u000D int x;` (escaped CR), then the line is code (wrong on `main`) | PASS |
| TC46 | Given escaped `//` with CRLF line endings, then the line is a comment (wrong on `main`) | PASS |
| TC47 | Given escaped `//` inside a text block, then the line is code | PASS |
| TC48 | Given unclosed `/**`, unclosed `/*` without newline, or a file ending in `/*`, then following lines are comments | PASS |
| TC49 | Given `"/*"` in a string or `/*` in a line comment before the unclosed comment, then only the real comment counts | PASS |
| TC50 | Given `"""` inside an unclosed comment, or an escaped `/*` that is not closed, then following lines are comments | PASS |
| TC51 | Given a syntax error and a last `// comment` without newline, then counts are the same as on `main` | PASS |
| TC52 | Given `Lower.Java` and `Mixed.jAvA`, then exit 0; `Notes.java.txt` and `java`, then exit 2 | PASS |
| TC53 | Given Turkish locale (JAR and native), when `loc FILE.JAVA`, then exit 0 | PASS |
| TC54 | Given 5000 LOC with a syntax error (two parser runs), then 0.11 s and 55 MB (valid file: 0.04 s, 36 MB) | PASS |
| TC55 | Given 5000 LOC with an unclosed comment or 555 escaped comments, then counts are correct and time stays < 0.1 s | PASS |

Observations (not blocking, same as on `main`):

| ID | Observation |
| --- | --- |
| O1 | Fixed in `9a34a3d`: a line with only `\u0020` counted as code; `javac` sees whitespace |
| O2 | Not fixed, documented in the issue: an unterminated string literal stops the lexer, so later line comments count as code |
| O3 | Fixed in `9a34a3d`: an invalid escape such as `\u00G1` in a comment was accepted without a warning; `javac` rejects the file |
| O4 | Fixed in `9a34a3d`: files with syntax errors were parsed twice (about 2.5x time of a valid file). The 1.5x memory comes from parser error recovery and stays |

## Re-test After Fixes O1, O3, O4

Fixes: unicode escapes that decode to whitespace count as whitespace (O1), an invalid
unicode escape sets the syntax error flag (O3), and the second parser run happens only
after a lexical error at the end of the file (O4).

- `make build`, `make test` (JUnit 50/50, pytest 12/12), `make coverage-codeinfo`
  (98.3%), `make test-codeinfo-native`: PASS
- 71 inputs compared with `e185b2e`: only the O1 and O3 inputs differ, as intended
- All F1/F2 re-test cases (TC40-TC51) still pass

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC60 | Given lines with only `\u0020`, `\u000C`, `\uuuu0020`, or `\u0020` with CRLF, then they are empty lines | PASS |
| TC61 | Given `// c \u0009`, then the line is a comment line | PASS |
| TC62 | Given `int\u0020a;`, then the line is code | PASS |
| TC63 | Given `\u00A0` (no-break space, not whitespace for `javac`), then the line is code | PASS |
| TC64 | Given `\\\u0020` (three backslashes) or `\u005cu0020`, then the line is code | PASS |
| TC65 | Given a `\u0020` only line inside a block comment, then it is an empty line, as a plain blank line there | PASS |
| TC66 | Given `\u` at line end, `\uGGGG`, `"\u00"` in a string, or fullwidth digits after `\u`, then the syntax error warning is printed | PASS |
| TC67 | Given `\u00e9`, `\U0041` (capital U is no escape) or `"\\\\u00G1"` (escaped backslash), then no warning | PASS |
| TC68 | Given a missing closing brace (parse error at end, not lexical), then counts are unchanged and no second run is needed | PASS |
| TC69 | Given locale `cs`, `tr`, `ja` (JAR) or `LANG=cs_CZ` (native), then an unclosed comment is still detected | PASS |
| TC70 | Given 5000 LOC with a syntax error, then 20.6 ms instead of 40.6 ms (in-process median) | PASS |
| TC71 | Given 5000 LOC with or without escapes, then time is unchanged (17.2 ms vs 16.8 ms) and memory 35.5 MB | PASS |

New finding (not blocking, behavior existed before for the raw character):

| ID | Severity | Finding |
| --- | --- | --- |
| N1 | Low, fixed in `9c9aed4` | `\u2028` (line separator) and the raw U+2028 character count as whitespace, because `Character.isWhitespace` is wider than Java whitespace (JLS 3.6: space, tab, form feed, line terminators); `javac` reports an illegal character |

## Re-test After Fix N1

Fix: whitespace follows JLS 3.6 (space, tab, form feed, line terminators) instead of
`Character.isWhitespace`, for raw characters and unicode escapes. `88d03ea` on top of
the fix only adds the detail file of #11 and changes no code.

- `make build`, `make test` (JUnit 52/52, pytest 12/12), `make coverage-codeinfo`,
  `make test-codeinfo-native`: PASS
- 91 inputs compared with `0ffc8ac`: only the two U+2028 inputs differ, as intended
- All F1/F2 cases (TC40-TC51) and O1/O3/O4 cases (TC60-TC71) still pass
- Time and memory unchanged (5000 LOC: 47.9 ms, 35.3 MB native)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC80 | Given each of the 21 characters that `Character.isWhitespace` accepts but JLS does not (U+000B, U+001C-U+001F, U+1680, U+2000-U+2006, U+2008-U+200A, U+2028, U+2029, U+205F, U+3000), raw and as unicode escape, then the line is code (42 cases) | PASS |
| TC81 | Given space, tab and form feed raw, or `\u0009`, `\u000C`, `\u0020`, `\u000A`, `\u000D`, then the line is empty | PASS |
| TC82 | Given U+3000 inside a line comment or a block comment, then the line stays a comment line | PASS |
| TC83 | Given U+3000 before `//`, then the line is code | PASS |
| TC84 | Given U+00A0 (no-break space) or a BOM, then counts are unchanged | PASS |

New observation (not blocking):

| ID | Observation |
| --- | --- |
| N2 | Lines with U+2028, U+3000 and similar characters count as code, but no syntax error warning is printed, because the JavaParser lexer accepts them; `javac` reports an illegal character |

## CI Notes

- `make coverage-codeinfo` fits CI (#10) as the codeinfo gate; it needs no GraalVM
- `make test-codeinfo-native` needs GraalVM and takes 1-2 minutes; run it on release
  branches or nightly
- Timing values are not stable on shared runners; check memory and exit codes in CI,
  and keep timing as a report only

## Not Tested

- Windows x64 build and binary (only documented)
- macOS
- Release packaging (`make dist-codeinfo-native`, `make deploy-local`)
