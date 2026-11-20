# Acceptance Protocol: Issue #016 — Code-Analysis Files Analyzed Count

**Issue**: #16 — code-analysis report shows a wrong "Files analyzed" count
**Branch**: `fix/016-files-analyzed-count`
**Tested commit**: `841ab7b`
**Date**: 2026-11-21
**Tester role**: QA/Test Engineer
**Environment**: Linux, Go 1.26.8, `ptx-codeanalysis` built with `make build-codeanalysis`
**STATUS**: PASS
**Approval for merge**: YES; no findings; Windows and macOS not tested (see
[Not Tested](#not-tested))

## Scope

`Files analyzed` in the console report is the number of unique files that match the
target patterns. The orchestrator sets the count, and `Results.Merge` does not add the
count of each engine to it.

## Plan

- Unit tests and coverage of the changed code (`make test-go`, `go test -cover`)
- Manual cases with the built binary on temporary projects: SonarLint only, CodeScan
  only, both engines, no engine, no matching file, more files, subdirectory and single
  file targets
- Baseline: the same projects with a binary built from `main`, to show the defect
- Failure injection: files that do not match the patterns, engines with no findings

## Cases

### Unit tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | `make test-go` | PASS, vet, build and all packages |
| TC02 | `TestMergeCombinesIssues`, `TestMergeKeepsFilesAnalyzed`, `TestMergeEmptyResults` | PASS |
| TC03 | `TestRunFilesAnalyzedCount`, `TestRunFilesAnalyzedNoMatch` | PASS |
| TC04 | `TestMergeKeepsFilesAnalyzed` with `types.go` from `main` | FAIL as expected, `FilesAnalyzed = 3, want 1` |

### Manual cases

Project from the issue: `src/A.java` with `// TODO: fix me`, `include: ["**/*.java"]`.

| ID | Given / When / Then | `main` | Fix |
| --- | --- | --- | --- |
| TC10 | Given SonarLint only, when `analyze`, then `Files analyzed: 1` (AC1) | 2 | PASS, 1 |
| TC11 | Given CodeScan only, when `analyze`, then `Files analyzed: 1` (AC1) | 2 | PASS, 1 |
| TC12 | Given both engines, when `analyze`, then `Files analyzed: 1`, `Issues found: 2` (AC1) | 3 | PASS, 1 |
| TC13 | Given no engine enabled, when `analyze`, then `Analyzed 1 file(s)` | — | PASS |
| TC14 | Given both engines and `include: ["**/*.py"]`, when `analyze`, then `Analyzed 0 file(s)` (AC2) | — | PASS |
| TC15 | Given both engines, `A.java`, `B.java` and `notes.md`, when `analyze`, then `Files analyzed: 2` (AC1) | 6 | PASS, 2 |
| TC16 | Given TC15, when `analyze ./src`, then `Files analyzed: 2` | — | PASS |
| TC17 | Given TC15, when `analyze src/A.java`, then `Files analyzed: 1` | — | PASS |

### Failure injection

| ID | Injection | Expected | Result |
| --- | --- | --- | --- |
| FI1 | A file that does not match the include pattern (`notes.md`) | not counted | PASS (TC15) |
| FI2 | Engines with no findings (`B.java` has no `TODO`) | the file is counted once | PASS (TC15) |
| FI3 | Missing bridge JAR | error, exit 3, no report | PASS, unchanged behavior |

## Coverage

| Acceptance criterion | Automated (Go) | Manual |
| --- | --- | --- |
| AC1 count with one and with both engines | `TestMergeKeepsFilesAnalyzed`, `TestRunFilesAnalyzedCount` | TC10-TC12, TC15-TC17 |
| AC2 `Analyzed 0 file(s)` | `TestRunFilesAnalyzedNoMatch` | TC14 |
| AC3 unit tests for `Merge` and `Run` | TC02, TC03 | — |

Coverage of the changed code: `Results.Merge` 100%, `NewResults` and `AddIssue` 100%,
`Orchestrator.Run` 50% (the engine branches need the bridge JARs and are covered by the
manual cases). The whole `internal/engine` package has 51.1%.

## Findings

No findings.

## CI Notes

- The new tests are Go unit tests and run with `make test-go`; no workflow change is
  needed
- `Run` with enabled engines has no automated test, because the engines are not
  injectable; an engine interface would make this testable without the bridge JARs

## Not Tested

- Windows
- macOS
