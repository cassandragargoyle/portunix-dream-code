# Acceptance Protocol: Issue #015 — Code-Analysis Target Patterns

**Issue**: #15 — code-analysis include and exclude patterns do not match
**Branch**: `fix/015-codeanalysis-target-patterns`
**Tested commit**: `01ef80c`
**Date**: 2026-11-21
**Tester role**: QA/Test Engineer
**Environment**: Linux, Go 1.26.8, `ptx-codeanalysis` built with `make build-codeanalysis`
**STATUS**: PASS
**Approval for merge**: YES; no blocking findings; P1 is an existing defect outside #15,
tracked in #16; Windows is covered only by unit tests (see [Not Tested](#not-tested))

## Scope

`spec.targets.include` and `spec.targets.exclude` are matched against the path relative
to the directory of the `.ptxcode` file, with `/` as separator. `**` matches zero or more
directories at any position. Exclude patterns with a trailing `/**` remove a whole
directory. A single file target is also checked against exclude patterns. Without a
`.ptxcode` file, patterns are relative to the analysis target.

## Plan

- Unit tests and coverage of the changed code (`make test-go`, `go test -cover`)
- Manual cases with the built binary on temporary projects, one per `init` template, the
  built-in default configuration, a subdirectory target, a single file target and
  `--config` from another directory
- The include and exclude patterns are taken unchanged from the `init` templates; only
  CodeScan is enabled, so that a `TODO` is reported in every language and each analyzed
  file appears in the report
- Failure injection: invalid pattern, empty include, symlink, special characters
- Regression: `analyze` on the `codeinfo` module of this repository

## Cases

### Unit tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | `make test-go` | PASS, vet, build and all packages |
| TC02 | `glob_test.go` and `orchestrator_test.go` | PASS, 51 subtests |
| TC03 | `TestCollectFilesSamplePtxcode` runs and is not skipped | PASS |

### Manual cases

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | Given the `java` template and a Maven layout with `generated/` and `target/`, when `analyze`, then `src/main/java/demo/A.java` and `src/test/java/demo/ATest.java` only (AC1, AC2) | PASS |
| TC11 | Given TC10, when `analyze ./src`, then the same files (patterns stay relative to `.ptxcode`) (AC5) | PASS |
| TC12 | Given no `.ptxcode`, `src/`, `target/gen/`, `node_modules/x/`, `targets/`, when `analyze`, then `src/A.java` and `targets/A.java` only (AC2) | PASS |
| TC13 | Given TC12 with `exclude: ["**/target/**", "**/node_modules/**"]`, when `analyze`, then the same files as TC12 (AC2) | PASS |
| TC14 | Given the `default` template, when `analyze`, then `src/A.java` and `lib/b.py`; no `other/`, `dist/`, `build/`, `node_modules/` (AC1, AC2) | PASS |
| TC15 | Given the `typescript` template, when `analyze`, then `app.ts`, `App.tsx`, `x.jsx`; no `*.test.ts`, `*.spec.ts`, `test/` (AC1, AC3) | PASS |
| TC16 | Given the `python` template, when `analyze`, then `mod.py` and `h.py`; no `test_*.py`, `__pycache__/`, `venv/`, `.venv/` (AC1, AC3) | PASS |
| TC17 | Given the `java` template, when `analyze target/gen/T.java`, then 0 files (AC6) | PASS |
| TC18 | Given the `java` template, when `analyze src/main/java/demo/A.java`, then 1 file (AC6) | PASS |
| TC19 | Given `--config` with a `.ptxcode` from another directory, when `analyze .`, then `src/A.java` only, `target/` excluded | PASS |
| TC20 | Given the `codeinfo` module with 11 source files under `target/`, when `analyze`, then no finding from `target/` | PASS |

### Failure injection

| ID | Injection | Expected | Result |
| --- | --- | --- | --- |
| FI1 | Invalid pattern `src/[.java` together with `./src/**/*.java` | no crash, `./src/**` matches | PASS, exit 0 |
| FI2 | `include: []` | 0 files | PASS, "Analyzed 0 file(s)" |
| FI3 | Symlink `src/node_modules` to a directory | the link is not followed | PASS |
| FI4 | Path with a space and diacritics | the file is found | PASS |

## Coverage

| Acceptance criterion | Automated (Go) | Manual |
| --- | --- | --- |
| AC1 templates and `sample.ptxcode` | `TestCollectFilesTemplates`, `TestCollectFilesSamplePtxcode` | TC10, TC14-TC16 |
| AC2 directory excludes | `TestMatchDirPattern`, `TestCollectFilesDefaultConfig` | TC10, TC12-TC14, TC20 |
| AC3 file-name-only patterns | `TestMatchPattern` | TC15, TC16 |
| AC4 Linux and Windows | `/` separator in all tests | Linux only |
| AC5 base of patterns documented | `TestCollectFilesSubdirectoryTarget` | TC11, `codeanalysis/README.md` |
| AC6 single file target | `TestCollectFilesSingleFile`, `TestCollectFilesSingleFileDefaultConfig` | TC17, TC18 |

Coverage of the changed code: `glob.go` 100%, `matchesPatterns`, `isExcluded`,
`isDirExcluded` 100%, `collectFiles` 90.5%, `relativePath` 88.9%. The whole
`internal/engine` package has 42.2%, because the SonarLint and CodeScan tests need the
bridge JARs; this is not changed by #15.

## Findings

No blocking findings.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Low | **Existing defect, outside #15.** `Files analyzed` in the console report is twice the real count with one engine, because `Results.Merge` adds the engine count to the orchestrator count. **Tracked** in #16 |

## CI Notes

- The new tests are Go unit tests and run with `make test-go`; no workflow change is
  needed
- Suggestion: add `windows-latest` with `make test-go` to the CI matrix for AC4

## Not Tested

- Windows (AC4 is covered only by unit tests that use `/` as separator)
- macOS
