# Acceptance Protocol: Issue #010 — Basic CI Checks for Pull Requests

**Issue**: #10 — Add basic CI checks for pull requests
**Branch**: `feature/010-ci-pr-checks`
**Tested commit**: `ae72504`, re-test of the finding fixes on `8deb485`
**Date**: 2026-10-27
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES, as an exception — merged before a real GitHub run (approved
by the user); the first run on `main` after the merge is green (see
[Real Run on GitHub](#real-run-on-github)); findings P1-P3 are fixed in `8deb485` and
re-tested

## Scope

The branch adds `.github/workflows/pr-checks.yml` with three parallel jobs
(`Java (codeinfo)`, `Go (codeanalysis)`, `Java bridge (SonarLint)`), a `test-go` target,
a fix of the `codeinfo` parent POM, and CI documentation in `TESTING.md`. The CodeScan
bridge and the Python tests are out of CI because `dream-code-devtools` is not in a
public Maven repository.

## Plan

- Static checks: YAML parse, `actionlint`, acceptance criteria against the files
- Clean-runner check: build and test the Java modules with an empty local Maven
  repository
- Failure injection on the `make` commands that CI runs, one case at a time
- Workflow simulation with `act` 0.2.80 (`pull_request` event, podman): green run with
  a cold cache, green run with a warm cache, red run with one injected failure per job
- Regression: `make build` and `make test` on the branch

### Simulation setup

`act` does not reproduce a GitHub runner exactly. Two changes were needed, both only in
a throwaway worktree:

- `act` 0.2.80 does not support the `node24` runtime, so the actions were lowered to
  `checkout@v4`, `setup-java@v4`, `setup-go@v5` (same inputs, `node20`)
- The `catthehacker/ubuntu:act-latest` image has no Maven; a local image adds Maven
  3.9.11 (GitHub `ubuntu-latest` has Maven 3.9.x preinstalled)

## Cases

### Static checks and acceptance criteria

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | When the workflow is parsed as YAML and checked with `actionlint`, then no errors | PASS |
| TC02 | Given the workflow, then triggers are `pull_request` and `push` to `main` (AC1) | PASS |
| TC03 | When `make help` and `make -C codeanalysis help`, then both list `test-go` (AC5) | PASS |
| TC04 | Given `TESTING.md`, then it describes the CI jobs and the manual checks (AC6) | PASS, see P1 |
| TC05 | Given the issue file, then it recommends branch protection with the three checks (AC7) | PASS |

### Clean runner

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | Given `main` and an empty local Maven repository, when `mvn validate`, then it fails: parent `org.cassandragargoyle.dream-code:root` cannot be resolved | Confirmed (bug fixed by the branch) |
| TC11 | Given the branch and an empty local Maven repository, when `mvn clean test`, then build succeeds and JUnit 76/76 pass | PASS |
| TC12 | Given an empty local Maven repository, when the SonarLint bridge is built, then it succeeds; the CodeScan bridge fails on `dream-code-devtools` | PASS (as documented) |

### Failure injection on the CI commands

Each case changes one file, runs the command that CI runs, and restores the tree.

| ID | Injection | Command | Expected | Result |
| --- | --- | --- | --- | --- |
| FI1 | `fail()` in codeinfo `MainTest` | `make test-java` | exit ≠ 0 | PASS (exit 2, `Failures: 1`) |
| FI2 | Syntax error in codeinfo `Main.java` | `make build-java` | exit ≠ 0 | PASS (exit 2, `COMPILATION ERROR`) |
| FI3 | Go syntax error | `make test-go` | exit ≠ 0 | PASS (exit 2, `syntax error`) |
| FI4 | `fmt.Printf("%d", "text")` | `make test-go` | `go vet` fails | PASS (exit 2, `wrong type string`) |
| FI5 | Go test with `t.Fatal` | `make test-go` | exit ≠ 0 | PASS (exit 2, `--- FAIL`) |
| FI6 | `fail()` in SonarLint bridge `MainTest` | bridge job command | exit ≠ 0 | PASS (exit 2, `Failures: 1`) |
| FI7 | Syntax error in SonarLint bridge `Main.java` | bridge job command | exit ≠ 0 | PASS (exit 2, `COMPILATION ERROR`) |
| FI0 | None (control) | `make test-go`, bridge job command | exit 0 | PASS |

### Workflow simulation (`act`)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Given a cold cache, when the workflow runs, then all three jobs succeed (AC3) | PASS, 185 s |
| TC21 | Given the cache from TC20, when the workflow runs again, then Maven and Go caches are restored and the run is faster | PASS, 137 s (codeinfo build 46 s → 20 s, Go step 24 s → 13 s) |
| TC22 | Given FI1 + FI3 + FI6 at once, when the workflow runs, then all three jobs fail and the log shows the cause (AC2) | PASS, 81 s |
| TC23 | Go version used by CI | `go1.21.13` from `codeanalysis/go.mod`; vet, build and tests pass |
| TC24 | JUnit in the Java job: 76 run, 1 skipped (`LinesOfCodeCommandTest` unreadable-file case, `assumeTrue` fails because `act` runs as root) | Expected, see P6 |

### Regression

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC30 | When `make build` on the branch, then it succeeds; `ptx-codeinfo.jar` keeps its name | PASS |
| TC31 | When `make test`, then JUnit 76/76 and pytest 12/12 pass | PASS |
| TC32 | Given the new groupId `org.cassandragargoyle.portunix-dream-code` of `ptx-codeinfo`, then no script, Makefile or doc refers to the old one | PASS |

## Coverage

The branch adds no product code, so coverage is unchanged (codeinfo 97.3%, gate 95%,
see #6). CI does not measure coverage (out of scope).

| Area | In CI | Manual |
| --- | --- | --- |
| codeinfo build and JUnit | yes | - |
| codeanalysis Go (vet, build, unit tests) | yes | - |
| SonarLint bridge build and JUnit | yes | - |
| CodeScan bridge, Python tests | no | `make test-codeanalysis` |
| Native image, coverage gate | no | `make test-codeinfo-native`, `make coverage-codeinfo` |
| Windows, macOS | no | manual |

## Findings

No blocking findings.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Low | `make test-go` is missing in the "Test Commands" table of `TESTING.md` and in `codeanalysis/TESTING.md`. **Fixed** in `8deb485` (R01) |
| P2 | Low | Both Java jobs use the same Maven cache key (hash of all `pom.xml` files). On GitHub only the first job saves the cache, so the other job's dependencies are not cached. Fix: `cache-dependency-path` per job (`pom.xml` + `codeinfo/pom.xml`, and `codeanalysis/bridges/sonarlint/pom.xml`). **Fixed** in `8deb485` (R03, R04) |
| P3 | Low | `cancel-in-progress: true` also applies to pushes to `main`: two quick pushes cancel the run of the first commit, so it gets no result. Suggestion: `cancel-in-progress: ${{ github.event_name == 'pull_request' }}`. **Fixed** in `8deb485` (R02) |
| P4 | Info | Maven comes from the runner image and is not pinned; a change of the image can change the Maven version. `MAVEN_ARGS` needs Maven 3.9+ |
| P5 | Info | CI uses Go 1.21.13 (minimum from `go.mod`, end of life), developers use newer Go (1.26 here). This checks the declared minimum, which is good; raising `go` in `go.mod` is a separate decision |
| P6 | Info | Under `act` (root) one JUnit test is skipped; on GitHub the job runs as user `runner`, so the test runs there |

## Re-test of `8deb485`

The commit fixes P1, P2 and P3. It changes only `pr-checks.yml`, `TESTING.md` and
`codeanalysis/TESTING.md`. Simulation setup as above (`node20` action versions, image
with Maven 3.9).

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R01 | Given `TESTING.md` and `codeanalysis/TESTING.md`, then `make test-go` is in the command table and in both command lists; `make -C codeanalysis test-go` passes | PASS |
| R02 | When `actionlint` checks the workflow, then no errors; `cancel-in-progress` is `${{ github.event_name == 'pull_request' }}` | PASS (runtime behavior on `main` not testable before merge) |
| R03 | Given an empty cache, when the workflow runs, then all three jobs succeed and the two Java jobs save different Maven keys (`982b…` bridge, `b684…` codeinfo) | PASS, 77 s |
| R04 | Given the cache from R03, when the workflow runs again, then each Java job restores its own key; codeinfo build 31.6 s → 5.6 s, bridge 20.6 s → 6.3 s | PASS, 50 s |
| R05 | Given FI1 + FI3 + FI6 at once, when the workflow runs, then all three jobs fail with the injected cause | PASS, 37 s |

The cold run is faster than TC20 because the JDK and Go downloads were already in the
local `act` tool cache; GitHub run times will differ.

## Real Run on GitHub

Run 40510064069, `push` to `main` after the fast-forward merge of `f778cea`, actions
`checkout@v5`, `setup-java@v5`, `setup-go@v6`. Total 38 s.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| G01 | When `f778cea` is pushed to `main`, then the workflow starts automatically | PASS |
| G02 | `Java (codeinfo)`: build and JUnit 76/76, 0 skipped (the unreadable-file test from P6 runs as user `runner`) | PASS, 33 s |
| G03 | `Go (codeanalysis)`: Go 1.21.13, vet, build and tests | PASS, 19 s |
| G04 | `Java bridge (SonarLint)`: build and JUnit 3/3 | PASS, 23 s |
| G05 | The two Java jobs save separate Maven cache keys (`982b…`, `b684…`), as in R03 | PASS |

## CI Notes

- Local reproduction: `act pull_request -W .github/workflows/pr-checks.yml` with a runner
  image that contains Maven 3.9 and `--container-daemon-socket -` for podman; `act`
  0.2.80 needs the `node20` action versions
- Run time is well under the 10-minute target (AC4) in the simulation; GitHub runners
  also download JDK and Go, so the first real run should be checked

## Not Tested

- A red check on a real GitHub PR (verified only with `act`); the first PR with a
  failing change will show it
- The `pull_request` trigger on GitHub (verified only with `act`)
- The cancellation rule from P3 on `main` (needs two quick pushes)
- Branch protection (admin setting)
