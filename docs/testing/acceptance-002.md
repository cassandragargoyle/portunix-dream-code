# Acceptance Protocol: Issue #002 — ptx-codeinfo Coverage, Memory, Help and Docs

**Issue**: #2 — Implement ptx-codeinfo CLI Application
**Branch**: `feature/002-ptx-codeinfo-polish`
**Tested commit**: `b1793f9`
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
| F1 | Low | Comments written with Unicode escapes (`// text`) are counted as code and produce a syntax error warning; `javac` treats them as comments |
| F2 | Low | An unclosed `/*` makes all following lines code lines; `javac` treats them as comment (the file does not compile) |
| F3 | Low | The `.java` check is case sensitive, so `Example.JAVA` is rejected; this may matter on Windows |
| F4 | Low | Memory grows with file size: a 50 000 line file needs 204 MB and 1.3 s; there is no target for this size |
| F5 | Known | Root `--help` and `-h` are rejected, tracked in #6 |

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
