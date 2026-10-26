# Issue #10: Add basic CI checks for pull requests

## Summary

Pull requests currently run no automated checks. Add a lightweight GitHub Actions
workflow that builds the Java modules and codeanalysis and runs the fast test suites
on every PR to `main`, so that broken builds or failing tests are visible before merge.

**Status**: 📋 Open
**Type**: enhancement
**Priority**: medium
**Component**: build / CI
**Created**: 2026-10-26
**GitHub**: #10
**Related**: #2 (Phase 6 CI was out of scope), #9

## Problem Description

The repository has no `.github/workflows/` directory. A PR can be merged with a
broken build or failing tests, and reviewers must build and test locally to find out.
Example: in #9 the native binary had been crashing on every valid Java file, and
nothing reported it.

The goal is a small first step, not a full CI/CD pipeline.

## Scope

One workflow, `.github/workflows/pr-checks.yml`, triggered on `pull_request` to `main`
and on `push` to `main`, running on `ubuntu-latest`:

1. **Java** – set up JDK 21 (Temurin), then:
   - `make build-java` (all Maven modules, native image skipped)
   - `make test-java` (JUnit, includes codeinfo)
2. **Go (codeanalysis)** – set up Go from `codeanalysis/go.mod`, then:
   - `go vet ./...`
   - `go build ./...`
   - `go test ./...` – add a `test-go` target to `codeanalysis/Makefile` and call it via `make`
3. **Python (codeanalysis)** – set up uv, then `make test-quick` (pytest `-m quick`)
4. **Caching** – Maven repository, Go module cache, uv cache

Jobs may run in parallel. All commands go through `make` where a target exists
(project convention).

### Out of Scope

- GraalVM native image build (slow; stays manual via `make test-codeinfo-native`)
- Windows / macOS runners
- Release automation and artifact upload
- Coverage measurement and thresholds
- Linters beyond `go vet` (can follow later)

## Acceptance Criteria

1. Workflow runs automatically on every PR to `main` and on push to `main`
2. A failing build or test makes the check red
3. A clean PR makes the check green
4. Full run takes less than 10 minutes
5. `make test-go` exists in `codeanalysis/Makefile` and is listed in `make help`
6. `TESTING.md` (or `README.md`) describes what CI checks and what stays manual
7. The issue notes a recommendation to enable branch protection (required checks)
   on `main` – the setting itself is done by a repository admin

## Testing

- Open a test PR with an intentionally failing JUnit test → check is red
- Open a test PR with an intentionally failing Go build → check is red
- Revert the failures → check is green
- Verify that the second run is faster thanks to caching
