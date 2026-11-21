# Issue #16: code-analysis report shows a wrong "Files analyzed" count

## Summary

The console report of `ptx-codeanalysis analyze` shows a wrong number of analyzed
files. With one engine the count is doubled, with SonarLint and CodeScan enabled it is
tripled.

**Status**: ✅ Implemented
**Type**: bug
**Priority**: low
**Component**: codeanalysis / engine
**Created**: 2026-11-21
**Closed**: 2026-11-21
**GitHub**: #16
**Related**: #15

## Problem Description

### Current State

A project with one file `src/A.java` that contains a `TODO` comment:

```bash
mkdir -p demo/src && cd demo
printf 'class A {\n// TODO: fix me\n}\n' > src/A.java
ptx-codeanalysis analyze
```

Result: `Files analyzed: 2`. With SonarLint and CodeScan enabled in `.ptxcode` the
report shows `Files analyzed: 3`. The expected value is `1`.

### Cause

`Orchestrator.Run` in `codeanalysis/internal/engine/orchestrator.go` sets
`results.Stats.FilesAnalyzed` to the number of collected files. Each engine returns its
own `Stats.FilesAnalyzed`, and `Results.Merge` in `codeanalysis/internal/engine/types.go`
adds it:

```go
r.Stats.FilesAnalyzed += other.Stats.FilesAnalyzed
```

### Possible Solution

Keep the count on the orchestrator, because it is the number of unique files that
match the target patterns. `Merge` does not add `FilesAnalyzed`.

## Acceptance Criteria

- [x] `Files analyzed` equals the number of files that match the target patterns, with
  one engine and with both engines enabled
- [x] `Analyzed 0 file(s)` stays correct when no file matches
- [x] Unit tests cover `Results.Merge` and the count in `Orchestrator.Run`; `Run` with
  enabled engines is covered only by manual cases (see `docs/testing/acceptance-016.md`)

## Testing

1. `make test-go`: new unit tests for `Merge`
2. The scenario from [Current State](#current-state) with SonarLint only, CodeScan only,
   and both engines
