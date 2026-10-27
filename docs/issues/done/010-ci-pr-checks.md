# Issue #10: Add basic CI checks for pull requests

## Summary

Pull requests currently run no automated checks. Add a lightweight GitHub Actions
workflow that builds the Java modules and codeanalysis and runs the fast test suites
on every PR to `main`, so that broken builds or failing tests are visible before merge.

**Status**: ✅ Implemented
**Type**: enhancement
**Priority**: medium
**Component**: build / CI
**Created**: 2026-10-26
**Closed**: 2026-10-27
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

- [x] 1. Workflow runs automatically on every PR to `main` and on push to `main`
  (push to `main` verified on GitHub; PR trigger verified with `act`)
- [x] 2. A failing build or test makes the check red (verified with `act` and failure
  injection; not yet seen on a real GitHub PR)
- [x] 3. A clean PR makes the check green (first run on `main` green)
- [x] 4. Full run takes less than 10 minutes (38 s on GitHub)
- [x] 5. `make test-go` exists in `codeanalysis/Makefile` and is listed in `make help`
- [x] 6. `TESTING.md` (or `README.md`) describes what CI checks and what stays manual
- [x] 7. The issue notes a recommendation to enable branch protection (required checks)
  on `main` – the setting itself is done by a repository admin (setting not done yet)

Acceptance: [docs/testing/acceptance-010.md](../../testing/acceptance-010.md)

## Implementation Notes

Findings during implementation that changed the scope:

- `codeinfo/pom.xml` used the parent `org.cassandragargoyle.dream-code:root`, which
  exists only in a developer's local Maven repository. On a clean runner the Java build
  failed while reading the POM. The parent now points to this repository's root POM
  (`org.cassandragargoyle.portunix-dream-code:root`); build and all JUnit tests pass
  with an empty local Maven repository.
- The CodeScan bridge depends on `dream-code-devtools`, which is not in a public Maven
  repository, so it cannot be built in CI. Both Python quick tests use this bridge and
  would only be skipped. Therefore the Python job (and the uv cache) is replaced by a
  job that builds and tests the SonarLint bridge (public dependencies only). CodeScan
  and Python tests stay manual until `dream-code-devtools` is published (follow-up
  issue).

## Recommendation: Branch Protection

After the workflow has run at least once, a repository admin should enable branch
protection on `main` (Settings → Branches, or a ruleset) and mark these checks as
required:

- `Java (codeinfo)`
- `Go (codeanalysis)`
- `Java bridge (SonarLint)`

Optionally also require pull requests before merging and branches to be up to date.

## Testing

- Open a test PR with an intentionally failing JUnit test → check is red
- Open a test PR with an intentionally failing Go build → check is red
- Revert the failures → check is green
- Verify that the second run is faster thanks to caching
