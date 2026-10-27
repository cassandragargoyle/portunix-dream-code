# Acceptance Protocol: Issue #012 — Make dream-code-devtools Available to CI

**Issue**: #12 — Make dream-code-devtools available to CI for the CodeScan bridge
**Branch**: `feature/012-devtools-ci-dependency`
**Tested commit**: `44ef9cf`
**Date**: 2026-10-27
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES, after the first real GitHub run on the PR is green (all four
checks); no blocking findings

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
| P1 | Low | The "Copied from dream-code-devtools" comments and `TESTING.md` do not name the library version that was copied (`1.0.0.4-SNAPSHOT`). Without it, a later sync cannot tell which library changes are missing. Suggestion: add the version to the class comments or to `TESTING.md` |
| P2 | Low | License is not consistent: the new SPDX headers and the Java file headers say MIT, `AGENTS.md` says "Proprietary - CassandraGargoyle team only", and there is no `LICENSE` file (the headers refer to one). Decision for the owner |
| P3 | Info | `astral-sh/setup-uv` does not pin a Python version, so CI uses the runner's Python (3.12.3). `requires-python` is `>=3.10`, so the minimum is not tested (same idea as P5 in #10) |
| P4 | Info | `make test` does not run the JUnit tests of the bridges (`test: test-java test-codeanalysis`). CI runs them, local `make test` does not. Existing behavior, not changed by the branch |
| P5 | Info | `dependency-reduced-pom.xml` is generated by the shade plugin but tracked in git, so every dependency change shows a generated diff. Existing behavior |
| P6 | Info | For `// TODO: a FIXME: b` the TODO message is `a FIXME: b`. Same as the library (TC20), not a regression |
| P7 | Info | `Rule.getIssues()` returns `List<CodeIssue<?>>` instead of the raw `List<CodeIssue>` of the library. Source-level improvement, no effect on the bridge |

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
