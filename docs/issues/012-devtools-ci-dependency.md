# Issue #12: Make dream-code-devtools available to CI for the CodeScan bridge

## Summary

The CodeScan bridge depends on `dream-code-devtools`, which is not in a public Maven
repository. CI cannot build the bridge, so the CodeScan bridge tests and the Python tests
run only manually. Make the dependency available to CI and add these checks to the
workflow from #10.

**Status**: 📋 Open
**Type**: enhancement
**Priority**: medium
**Component**: codeanalysis / build / CI
**Created**: 2026-10-27
**GitHub**: #12
**Related**: #10

## Problem Description

`codeanalysis/bridges/codescan/pom.xml` depends on
`org.cassandragargoyle.dream-code:dream-code-devtools:1.0.0.4-SNAPSHOT`. The artifact
exists only in a developer's local Maven repository (`mvn install` from another
project). On a clean machine the build fails:

```text
Could not resolve dependencies for project
org.cassandragargoyle.portunix:ptx-codescan-bridge:jar:1.0.4-SNAPSHOT
dependency: org.cassandragargoyle.dream-code:dream-code-devtools:jar:1.0.0.4-SNAPSHOT
```

Consequences for CI (#10):

- The CodeScan bridge is not built or tested
- The Python tests in `codeanalysis/test/` use the bridge JAR. Without it they are
  skipped (`pytest.skip`), so a Python job would be green while testing nothing.
  Therefore `make test-quick` is not in CI at all
- `TESTING.md` lists these checks under "Manual checks (not in CI)"

The bridge uses only four classes from the library (package
`org.cassandragargoyle.devtools.code`): `ClientFile`, `CodeIssue`,
`CommentAnnotationIssue`, `RuleFindCommentAnnotations`.

## Options

| Option | Description | Pros | Cons |
| --- | --- | --- | --- |
| A | Publish `dream-code-devtools` to GitHub Packages (Maven registry) | Single source of the code; CI reads it with `GITHUB_TOKEN` and `permissions: packages: read` | Every local build needs a token in `~/.m2/settings.xml`, also for public read; publishing must be set up in the library's project |
| B | Publish to Maven Central | No token for anyone | Most work: release versions instead of SNAPSHOT, signing, namespace verification |
| C | Move the four used classes into the CodeScan bridge | Simplest for CI and local builds, no external dependency | Code exists in two places; changes in the library must be copied by hand |

The choice is a decision for this issue (architect / tech lead). Option C is the
smallest change if the bridge is the only user of these classes in this repository.

## Scope

1. Make the dependency resolvable on a clean runner (option chosen above)
2. Extend `.github/workflows/pr-checks.yml`:
   - Build and test the CodeScan bridge (can join the `Java bridge (SonarLint)` job or
     get its own job with its own Maven cache key)
   - Python job: set up uv (with cache), build the bridges, run `make test-quick`
3. Update `TESTING.md` ("Continuous Integration" and "Manual checks")

### Out of Scope

- Full integration tests (`make test-integration`) in CI, if they take too long
- Changes to the library itself beyond publishing

## Acceptance Criteria

1. On a clean machine (empty local Maven repository) the CodeScan bridge builds
2. CI has a check for the CodeScan bridge build and tests
3. CI runs `make test-quick`; the quick tests run and are not skipped
4. A failing CodeScan bridge test or Python quick test makes the check red
5. The local build without credentials still works, or `TESTING.md` describes the
   one-time setup
6. `TESTING.md` is updated: the CodeScan bridge and the Python quick tests are no longer
   listed as manual
7. The full CI run still takes less than 10 minutes

## Testing

- Build the CodeScan bridge with `-Dmaven.repo.local=<empty directory>`
- Run the workflow locally with `act` (setup in `docs/testing/acceptance-010.md`) and
  on a real PR
- Failure injection: a failing CodeScan bridge test and a failing Python quick test
  must turn the related check red
- Check in the pytest output that the quick tests passed, not skipped
