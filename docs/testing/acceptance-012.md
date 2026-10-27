# Acceptance Protocol: Issue #012 — Make dream-code-devtools Available to CI

**Issue**: #12 — Make dream-code-devtools available to CI for the CodeScan bridge
**Branch**: `feature/012-devtools-ci-dependency`
**Tested commit**: `44ef9cf`, re-test of the finding fixes on `92066a3`, `17b292c`, `9176012`, `55c6d50`, `fc7dc19` and `38cd02b`
**Date**: 2026-10-27
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES, after the first real GitHub run on the PR is green (all four
checks); no blocking findings; P1, P3, P4, P5 are fixed in `92066a3` and re-tested
(see [Re-test of `92066a3`](#re-test-of-92066a3)); P8 is fixed in `17b292c` and re-tested (see
[Re-test of `17b292c`](#re-test-of-17b292c)); P2, P9, P10 are fixed in `9176012` and
re-tested (see [Re-test of `9176012`](#re-test-of-9176012)); P11, P12, P13 are fixed in `55c6d50` and
`fc7dc19` and re-tested (see [Re-test of `55c6d50` and `fc7dc19`](#re-test-of-55c6d50-and-fc7dc19));
P14 is fixed in `38cd02b` and re-tested (see [Re-test of `38cd02b`](#re-test-of-38cd02b)); no finding is open

## Scope

The branch implements option C of the issue: the six types the CodeScan bridge used from
`dream-code-devtools` are copied into
`org.cassandragargoyle.portunix.codeanalysis.codescan.devtools`, and `javaparser-core`
3.27.1 is a direct dependency. CI gets one job for both Java bridges and a new Python
job with `make test-quick`. With `PTX_REQUIRE_BRIDGES=1` (set in CI) a missing bridge JAR
or Java fails the Python tests instead of skipping them. `TESTING.md` and the issue file
are updated, and SPDX headers are added.

## Plan

- Static checks: acceptance criteria against the files, no reference to the old
  dependency
- Clean-runner check: build the CodeScan bridge with an empty local Maven repository,
  check the content of the shaded JAR
- Regression of the copied code: run the old bridge (built from `main` with
  `dream-code-devtools` 1.0.0.4-SNAPSHOT) and the new bridge on the same files and
  compare the full JSON output
- Failure injection on the commands that CI runs, one case at a time, in a separate
  worktree
- Workflow simulation with `act` 0.2.80 (`pull_request` event, podman): cold cache, warm
  cache, red run with injected failures
- Regression: `make build` and `make test` on the branch

### Simulation setup

Same as in [acceptance-010.md](acceptance-010.md#simulation-setup): `checkout@v4`,
`setup-java@v4`, `setup-go@v5` in a throwaway worktree, and the local image
`act-ubuntu-maven` (adds Maven 3.9). `astral-sh/setup-uv@v6` runs unchanged.

### Regression corpus

| Corpus | Files | Content |
| --- | --- | --- |
| Edge cases | 5 | CRLF line ends, Unicode and emoji, Javadoc, annotation in a string literal, two annotations on one line, file with a parse error (line-by-line fallback), empty file, comment before `package` |
| Repository | 42 | All tracked `*.java`, `*.py`, `*.ts` files |
| JDK 21 sources | 6325 | `java.base` and `java.desktop` from the JDK `src.zip` |

Request: all files at once, annotations `TODO`, `FIXME`, `HACK`, `XXX`, `NOTE`, `BUG`.
The issues are sorted and compared as JSON.

## Cases

### Static checks and acceptance criteria

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | Given the branch, when searching for `dream-code-devtools` and `devtools.code`, then only documentation and the "Copied from" comments refer to them | PASS |
| TC02 | Given `pom.xml`, then it has no `dream-code-devtools` and has `javaparser-core` 3.27.1 (the version that came through the library) | PASS |
| TC03 | Given the workflow, then the `bridge` job runs `build test` for both bridges and the `python` job runs `make test-quick` with `PTX_REQUIRE_BRIDGES=1` (AC2, AC3) | PASS |
| TC04 | Given `TESTING.md`, then the CI table lists both new checks, `PTX_REQUIRE_BRIDGES` is described, and the CodeScan bridge and quick tests are no longer manual (AC6) | PASS |
| TC05 | `actionlint` on the workflow | Not run, see [Not Tested](#not-tested) |

### Clean runner

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | Given an empty local Maven repository, when `mvn clean package` in `codeanalysis/bridges/codescan`, then build succeeds and JUnit 9/9 pass (AC1) | PASS |
| TC11 | Given the shaded JAR, then it has no class from `org/cassandragargoyle/devtools/` and has the seven copied classes and `StaticJavaParser` | PASS (0 old classes; the old JAR had 28) |
| TC12 | Given no credentials and no `~/.m2/settings.xml` entry, when the bridge is built, then it succeeds (AC5) | PASS (same run as TC10) |

### Regression of the copied code

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Given the edge-case corpus, when old and new bridge analyze it, then the output is identical | PASS, 12 = 12 issues |
| TC21 | Given the repository corpus, then the output is identical | PASS, 64 = 64 issues |
| TC22 | Given the JDK corpus, then the output is identical | PASS, 501 = 501 issues |
| TC23 | Given `String s = "FIXME: string"; // FIXME trailing`, then only the comment is reported | PASS |
| TC24 | Given CRLF line ends, then messages have no `\r` and line numbers are correct | PASS |
| TC25 | Given a file with a parse error, then the line-by-line fallback reports the `TODO` | PASS |
| TC26 | Given a block comment over several lines, then the line of the annotation (not of the comment start) is reported | PASS |

### Failure injection on the CI commands

Each case changes one file in a separate worktree, runs the command that CI runs, and
restores the tree. Bridge command: `make -C codeanalysis/bridges build test`. Python
command: `make -C codeanalysis/bridges build-codescan && PTX_REQUIRE_BRIDGES=1 make test-quick`.

| ID | Injection | Command | Expected | Result |
| --- | --- | --- | --- | --- |
| FI0 | None (control) | bridge, Python | exit 0 | PASS (JUnit 9/9, pytest 4 passed) |
| FI1 | `fail()` in `RuleFindCommentAnnotationsTest` | bridge | exit ≠ 0 | PASS (exit 2) |
| FI2 | Syntax error in `RuleFindCommentAnnotations` | bridge | exit ≠ 0 | PASS (exit 2) |
| FI3 | Behavior change: `TODO` pattern changed to `TODOX` | bridge | JUnit fails | PASS (exit 2) |
| FI3b | Same change as FI3 | Python | quick test fails | PASS (exit 2, 1 failed) |
| FI4 | `assert False` in a quick test | Python | exit ≠ 0 | PASS (exit 2, 1 failed) |
| FI5 | Bridge JAR removed, `PTX_REQUIRE_BRIDGES=1` | `make test-quick` | errors, not skips | PASS (exit 2, 4 errors) |
| FI6 | No `java` on `PATH`, `PTX_REQUIRE_BRIDGES=1` | `uv run pytest -m quick` | errors, not skips | PASS (exit 1, 4 errors) |
| FI7 | No `java` on `PATH`, variable not set (local run) | `uv run pytest -m quick` | skips, exit 0 | PASS (4 skipped) |

### Workflow simulation (`act`)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC30 | Given a cold cache, when the workflow runs, then all four jobs succeed | PASS, 116 s |
| TC31 | In TC30, the Python job runs the quick tests, they pass and none is skipped (AC3) | PASS, 4 passed, 0 skipped, Python 3.12.3, uv 0.13.6 |
| TC32 | In TC30, the bridge job runs both bridges' JUnit tests | PASS, SonarLint 3/3, CodeScan 9/9 |
| TC33 | The three Java jobs save separate Maven cache keys; the uv cache is saved under a key from `uv.lock` | PASS (`b684…`, `370e…`, `6c12…`; `setup-uv-1-…-2ec7…`) |
| TC34 | Given the cache from TC30, when the workflow runs again, then all caches are restored and the run is faster | PASS, 67 s |
| TC35 | Given FI1 + FI4 at once, when the workflow runs, then the bridge job and the Python job fail and the log shows the cause; the other jobs succeed (AC4) | PASS, 53 s |
| TC36 | Total run time (AC7) | PASS in simulation: 116 s cold, 67 s warm; the jobs run in parallel |

### Regression

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC40 | When `make build` on the branch, then it succeeds | PASS |
| TC41 | When `make test`, then JUnit 76/76 and pytest 12/12 pass | PASS, 10 s |
| TC42 | After all cases, the working tree is clean | PASS |

## Coverage

The bridge has no coverage tool (JaCoCo is configured only for `codeinfo`). Assessment by
reading the tests:

| Class | Covered by `RuleFindCommentAnnotationsTest` | Not covered |
| --- | --- | --- |
| `RuleFindCommentAnnotations` | All four annotation types, line comments, block comments, Javadoc via TC20-TC22, string literals, word boundaries, `init` reset | `execute()` with a `null` compilation unit |
| `CommentAnnotationIssue` | Getters, `getInputFile`, `getValue` | - |
| `AnnotationData` | `getMessage` | `getAnnotationType`, `getLine` (trivial getters) |
| `ClientFile` | `uri()` with a file | `uri()` with `null` |

The output comparison on 6372 files (TC20-TC22) is stronger evidence of equivalence than
the unit tests.

| Area | In CI | Manual |
| --- | --- | --- |
| codeinfo build and JUnit | yes | - |
| codeanalysis Go | yes | - |
| SonarLint bridge build and JUnit | yes | - |
| CodeScan bridge build and JUnit | yes (new) | - |
| Python quick tests | yes (new) | - |
| Full Python suite (`make test-codeanalysis`) | no | yes |
| Native image, coverage gate, Windows, macOS | no | yes |

## Findings

No blocking findings.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Low | The "Copied from dream-code-devtools" comments and `TESTING.md` do not name the library version that was copied (`1.0.0.4-SNAPSHOT`). Without it, a later sync cannot tell which library changes are missing. Suggestion: add the version to the class comments or to `TESTING.md`. **Fixed** in `92066a3`: the version is in `TESTING.md` and the issue file; the "Copied from" comments are removed from the code (owner decision) (R01) |
| P2 | Low | License is not consistent: the new SPDX headers and the Java file headers say MIT, `AGENTS.md` says "Proprietary - CassandraGargoyle team only", and there is no `LICENSE` file (the headers refer to one). Decision for the owner. **Fixed** in `9176012`: owner chose MIT; `LICENSE` added, `AGENTS.md`, both README files and `codeinfo/plugin.json` changed to MIT (R20-R23) |
| P3 | Info | `astral-sh/setup-uv` does not pin a Python version, so CI uses the runner's Python (3.12.3). `requires-python` is `>=3.10`, so the minimum is not tested (same idea as P5 in #10). **Fixed** in `92066a3`: `python-version: '3.10'` (R02) |
| P4 | Info | `make test` does not run the JUnit tests of the bridges (`test: test-java test-codeanalysis`). CI runs them, local `make test` does not. Existing behavior, not changed by the branch. **Fixed** in `92066a3`: new target `test-bridges`, part of `make test` (R03) |
| P5 | Info | `dependency-reduced-pom.xml` is generated by the shade plugin but tracked in git, so every dependency change shows a generated diff. Existing behavior. **Fixed** in `92066a3`: `createDependencyReducedPom=false` in both bridges (as in `codeinfo`), files removed from git and added to `.gitignore` (R04) |
| P6 | Info | For `// TODO: a FIXME: b` the TODO message is `a FIXME: b`. Same as the library (TC20), not a regression |
| P7 | Info | `Rule.getIssues()` returns `List<CodeIssue<?>>` instead of the raw `List<CodeIssue>` of the library. Source-level improvement, no effect on the bridge |
| P8 | Medium | **Existing defect, outside #12** (found in the re-test). The rule `ptx-codeanalysis` in `codeanalysis/.gitignore` (meant for the binary) also ignores the source directory `codeanalysis/cmd/ptx-codeanalysis/`, so `main.go` is not in git, also not on `main`. On a clean checkout `make build` fails (`can't cd to cmd/ptx-codeanalysis`). CI does not see it: `go build ./...` builds only the packages that exist. Suggestion: separate issue; change the rule to `/ptx-codeanalysis` and `/ptx-codeanalysis.exe`, commit `main.go`, and add `make build-codeanalysis` (or `go build ./cmd/...` with a check that the package exists) to the Go job. **Fixed** in `17b292c`: rules anchored to `/ptx-codeanalysis` and `/ptx-codeanalysis.exe`, `main.go` added to git (R10-R16) |
| P9 | Low | `codeanalysis/cmd/ptx-codeanalysis/main.go` (added in `17b292c`) has no license header and no SPDX line, unlike the other files changed in #12. No Go file in the repository has a header yet, so there is no Go convention. The SPDX value depends on P2. **Fixed** in `9176012` (R24) |
| P10 | Info | A binary built inside `cmd/ptx-codeanalysis/` with a plain `go build` (no `-o`) is named `ptx-codeanalysis` and is no longer ignored after the P8 fix. The `Makefile` always writes the binary to `codeanalysis/`, so this happens only with manual builds. **Fixed** in `9176012`: both names ignored in `cmd/ptx-codeanalysis/` (R25) |
| P11 | Low | Release archives from `scripts/make-release.py` contain the binary, the bridge JARs, `README.md` and `plugin.json`, but not `LICENSE`. The MIT License requires the notice in all copies. Suggestion: copy `LICENSE` into the archive. **Fixed** in `55c6d50` (R30, R31) |
| P12 | Info | Existing files still have older headers: 5 Java files have the MIT text without the SPDX line, 15 Go files have no header. The license is consistent (MIT); adding SPDX is a cleanup for a separate change. **Fixed** in `fc7dc19`: all 39 Java and Go source files have the MIT header with SPDX (R33) |
| P13 | Low | Existing defect, found by the developer during the P11 fix: the archive filter skipped every `.md` and `.txt` file, so `README.md` was not in any release archive, although the release notes list it. **Fixed** in `fc7dc19`: new `is_package_file()` skips only archives, `checksums_*` and `RELEASE_NOTES_*` in the package root (R30, R32) |
| P14 | Info | `gofmt -l` lists four Go files (`config.go`, `validator_test.go`, `codescan.go`, `sonarlint.go`, struct tag alignment). They were unformatted before this branch; the headers add no new case (R34). **Fixed** in `38cd02b`: `gofmt -w`, whitespace only (R40-R44) |

## Re-test of `92066a3`

The commit fixes P1, P3, P4 and P5. It changes the workflow, `Makefile`, `.gitignore`,
`TESTING.md`, the issue file, both bridge POMs, the five copied classes (comments only),
and removes both `dependency-reduced-pom.xml` files. Simulation setup as above.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R01 | Given the code, then no "Copied from" comment is left; `TESTING.md` and the issue file name `1.0.0.4-SNAPSHOT` (P1) | PASS |
| R02 | Given the workflow with `python-version: '3.10'`, when the Python job runs, then it uses Python 3.10 and the quick tests pass (P3) | PASS, Python 3.10.22, 4 passed |
| R03 | Given `make help`, then it lists `test-bridges`, and `make test` runs it (P4) | PASS |
| R04 | Given an empty local Maven repository, when both bridges are built, then they succeed, no `dependency-reduced-pom.xml` is created, and git has none (P5) | PASS, SonarLint 3/3, CodeScan 9/9 |
| R05 | Given the old and the new bridge, when the JDK and edge-case corpora are analyzed, then the output is identical (no change from P5) | PASS, 501 = 501, 12 = 12 |
| RF0 | `make test` without injection (control) | PASS, exit 0 |
| RF1 | `fail()` in the SonarLint bridge `MainTest`, then `make test` | PASS, exit 2, `Failures: 1` |
| RF2 | `fail()` in `RuleFindCommentAnnotationsTest`, then `make test` | PASS, exit 2, `Failures: 1` |
| R06 | Full workflow with `act`: all four jobs succeed | PASS, 147 s; codeinfo 76 run, 1 skipped (P6 in #10, `act` runs as root) |
| R07 | `assert False` in a quick test, Python job with `act` on Python 3.10 | PASS, job failed, `1 failed, 3 passed` |
| R08 | `make build` on a clean worktree of `92066a3` | FAIL, see P8 (existing defect, same on `main`, not caused by this branch); re-tested as R10 |

## Re-test of `17b292c`

The commit fixes P8: it anchors the ignore rules in `codeanalysis/.gitignore` and adds
`cmd/ptx-codeanalysis/main.go`. All cases on a clean worktree of `17b292c`.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R10 | Given a clean checkout, when `make build`, then it succeeds and the binary runs | PASS, `ptx-codeanalysis version 1.0.0` |
| R11 | Given the new rules, then `main.go` is not ignored, and `codeanalysis/ptx-codeanalysis` and `codeanalysis/ptx-codeanalysis.exe` are ignored | PASS |
| R12 | Given a build of the Linux binary and a Windows `.exe` in `codeanalysis/`, then `git status` shows no new file | PASS, 0 lines |
| R13 | Syntax error in `main.go`, then `make test-go` | PASS, exit 2 (before the fix the package was missing and the CI command could not see it) |
| R14 | Wrong `Fprintf` format in `main.go`, then `make test-go` | PASS, exit 2, `go vet` reports it |
| R15 | `make test` on the clean worktree | PASS, JUnit 76/76, bridges 3/3 and 9/9, pytest 12/12 |
| R16 | Full workflow with `act`, then the same syntax error as R13 in the Go job | PASS, all four jobs green in 94 s, the Go job lists `cmd/ptx-codeanalysis`; Go job red with the error from `main.go` |

## Re-test of `9176012`

The commit sets the MIT License (P2), adds the header to `main.go` (P9) and ignores the
binary of a plain `go build` in `cmd/ptx-codeanalysis/` (P10).

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R20 | Given `LICENSE`, then the text is the standard MIT text (compared with the MIT `LICENSE` of a Go module) apart from the copyright line `Copyright (c) 2026 CassandraGargoyle` | PASS |
| R21 | Given the repository, then no file states "Proprietary" as the project license | PASS (one generic mention of "proprietary code" in `AI-ASSISTANTS.md` is not a license statement) |
| R22 | All 22 SPDX lines say `MIT`; the 24 headers that refer to the "LICENSE file" now point to an existing file | PASS |
| R23 | Both `plugin.json` files are valid JSON with `"license": "MIT"`; the LICENSE links in `AGENTS.md`, `README.md`, `codeinfo/README.md` resolve | PASS |
| R24 | Given `main.go`, then it has the SPDX header, `gofmt` and `go vet` report nothing, and the header is not taken as package documentation | PASS |
| R25 | Given `go build` and a Windows `go build` inside `cmd/ptx-codeanalysis/`, then `git status` shows no new file and `main.go` is still tracked | PASS, 0 lines |
| R26 | Clean worktree: `make build`, `make test` | PASS, JUnit 76/76, bridges 3/3 and 9/9, pytest 12/12; tree clean |
| R27 | Full workflow with `act` | PASS, all four jobs green, 68 s |

## Re-test of `55c6d50` and `fc7dc19`

`55c6d50` copies `LICENSE` into both release packages (P11). `fc7dc19` adds the SPDX
headers (P12) and replaces the extension filter of the archives (P13). Release packages
were built in a throwaway worktree, so the local `dist/` was not touched.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R30 | When the codeanalysis (Linux, Windows) and codeinfo releases are built, then every `.tar.gz` and `.zip` contains `LICENSE` and `README.md`, and no archive, checksum or release notes file | PASS (codeanalysis: binary, 2 bridges, `defaults/sample.ptxcode`, `plugin.json`, `LICENSE`, `README.md`; codeinfo: JAR, `plugin.json`, `LICENSE`, `README.md`) |
| R31 | The "Package Contents" in the release notes match the archive content | PASS |
| R32 | `is_package_file()` on 12 cases: `README.md`, `LICENSE`, `NOTICE.txt`, nested `.md` and `.zip` are included; `RELEASE_NOTES_*`, `checksums_*`, `.tar.gz`, `.zip` in the root and directories are excluded | PASS 12/12 |
| R33 | All 39 tracked Java and Go source files have `SPDX-License-Identifier: MIT`; the 11 Java test fixtures and test data files have no header on purpose (tests count their lines) | PASS |
| R34 | `gofmt -l` reports the same files as before the headers; `go vet` passes; `go doc` of the four Go packages does not show the header | PASS |
| R35 | Two more codeinfo releases in a row: the archive still has 4 files, `sha256sum -c` passes | PASS |
| R36 | Clean worktree: `make build`, `make test`, `make test-go`; tree clean afterwards | PASS, JUnit 76/76, bridges 3/3 and 9/9, pytest 12/12 |
| R37 | Full workflow with `act` | PASS, all four jobs green, 77 s |

## Re-test of `38cd02b`

The commit runs `gofmt -w` on the four files from P14.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R40 | Given the diff to `7c11a08`, then it changes only the four files and `git diff -w` is empty (whitespace only) | PASS, 14 lines changed, 0 non-whitespace lines |
| R41 | When the binary is built with `-trimpath` from `7c11a08` and from `38cd02b`, then both binaries are identical | PASS, same SHA-256 |
| R42 | `gofmt -l codeanalysis` reports nothing; `make test-go` passes | PASS |
| R43 | Clean worktree: `make build`, `make test` | PASS, JUnit 76/76, bridges 3/3 and 9/9, pytest 12/12 |
| R44 | Go job with `act` | PASS |

## CI Notes

- Local reproduction as in #10: `act pull_request -W .github/workflows/pr-checks.yml`
  with `-P ubuntu-latest=localhost/act-ubuntu-maven:latest --container-daemon-socket -`
  and `node20` action versions
- `act` runs as root; on GitHub the jobs run as user `runner`. The Python job does not
  depend on the user
- The first real run on the PR should confirm the time and that the four checks appear
- `main` has no branch protection now (checked with `gh api`). When it gets required
  checks, use `Python quick tests (codeanalysis)` and the new name
  `Java bridges (SonarLint, CodeScan)`; the old `Java bridge (SonarLint)` check no longer
  exists

## Not Tested

- `actionlint` (the tool is not installed; running it in a container was not allowed in
  this session). The workflow structure is covered by the `act` runs
- A real GitHub run (`pull_request` trigger, GitHub cache service, `node24` actions)
- Windows and macOS
