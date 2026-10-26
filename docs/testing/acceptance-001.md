# Acceptance Protocol: Issue #001 — Picocli Version Alignment

**Issue**: #1 — Migrate CLI Framework to Picocli
**Branch**: `feature/001-picocli-version-alignment`
**Tested commit**: `e060406`
**Date**: 2026-10-26
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES

## Scope

All Java CLIs already used Picocli; the remaining work was aligning the version.
Picocli is bumped to 4.7.7 in `codeinfo` (was 4.7.5) and in the `sonarlint` and
`codescan` bridges (were 4.7.6).

## Plan

- Build with `make build`, run `make test` and `make -C codeanalysis/bridges test`
- Verify the bundled Picocli version in all three JARs
- Run 21 CLI smoke cases on the branch and on `main`; only a difference between
  the two runs counts as a regression

## Cases

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC00 | Given built JARs, when the bundled Picocli is checked, then all three contain 4.7.7 | PASS |
| TC01–02 | When `ptx-codeinfo --version` / `-v`, then it prints `1.0.2`, exit 0 | PASS |
| TC03, 12, 17 | When `--help-ai`, then it prints valid JSON, exit 0 | PASS |
| TC04, 06, 09 | When `help`, `help loc` or no arguments, then usage is printed, exit 0 | PASS |
| TC05 | When `loc <file>`, then it prints the line count | PASS |
| TC07 | When `loc` on a missing file, then it prints an error, exit 2 | PASS |
| TC08, 13, 18 | When an unknown option is given, then it prints an error with usage, exit 2 | PASS |
| TC10–11, 15–16 | When bridge `--help` / `-V`, then exit 0 | PASS |
| TC14, 19 | When `rules --help` / `analyze --help`, then subcommand help is printed | FAIL (pre-existing, same on `main`) |

## Results

- JUnit: codeinfo 10/10, SonarLint bridge 3/3, CodeScan bridge 3/3
- pytest: 12/12
- Smoke cases: identical results on `main` and on the branch — no regression

## Known Pre-existing Defects (out of scope)

- `ptx-codeinfo --help`, `-h` and `-V` are rejected: the custom `-v/--version`
  option collides with `mixinStandardHelpOptions`, so the whole mixin is dropped
- `rules --help` (SonarLint bridge) and `analyze --help` (CodeScan bridge) are rejected:
  the subcommands do not enable `mixinStandardHelpOptions`

## Not Tested

- `ptx-codeinfo` native image (`GRAALVM_HOME` not set)
- Windows
