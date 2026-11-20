# Issue #15: code-analysis include and exclude patterns do not match

## Summary

Target patterns in `.ptxcode` only work in the form `**/<file pattern>`. Include patterns
with a directory prefix never match, so `analyze` with a configuration from `init`
analyzes no file. Exclude patterns for directories never match, so `target/`,
`node_modules/` and similar directories are analyzed, also with the default
configuration.

**Status**: 📋 Open
**Type**: bug
**Priority**: high
**Component**: codeanalysis / engine
**Created**: 2026-11-20
**GitHub**: #15
**Related**: -

## Problem Description

### Background

`spec.targets.include` and `spec.targets.exclude` in `.ptxcode` are glob patterns. The
`init` templates (`internal/config/templates.go`) and `defaults/sample.ptxcode` use:

| Template | Include | Exclude |
| -------- | ------- | ------- |
| `default` | `src/**/*`, `lib/**/*` | `**/node_modules/**`, `**/build/**`, `**/target/**`, `**/dist/**` |
| `java` | `src/main/java/**/*.java`, `src/test/java/**/*.java` | `**/target/**`, `**/generated/**` |
| `typescript` | `src/**/*.ts`, `src/**/*.tsx`, `src/**/*.js`, `src/**/*.jsx` | `**/node_modules/**`, `**/dist/**`, `**/build/**`, `**/*.test.ts`, `**/*.spec.ts` |
| `python` | `src/**/*.py`, `lib/**/*.py` | `**/__pycache__/**`, `**/venv/**`, `**/.venv/**`, `**/test_*.py` |
| `sample.ptxcode` | `src/**/*.java`, `src/**/*.ts`, `src/**/*.js`, `src/**/*.py` | - |
| built-in default (no `.ptxcode`) | `**/*.java`, `**/*.ts`, `**/*.js`, `**/*.py` | `**/node_modules/**`, `**/build/**`, `**/target/**` |

### Current State

Tested with `ptx-codeanalysis` 1.1.1 on Linux.

#### Scenario 1: `java` template, Maven layout

```bash
mkdir -p demo/src/main/java/demo && cd demo
printf 'package demo;\n// TODO: fix me\npublic class A {}\n' > src/main/java/demo/A.java
ptx-codeanalysis init --template java
ptx-codeanalysis analyze
```

Result: "No issues found, Analyzed 0 file(s)". The same with the `default` template.

#### Scenario 2: exclude patterns

A directory with `src/A.java`, `target/gen/A.java` and `node_modules/x/A.java`, each with
a `TODO` comment. Without `.ptxcode`, and with
`exclude: ["**/target/**", "**/node_modules/**"]`, all three files are analyzed.

Without `.ptxcode` the include patterns work, because they all start with `**/`.

### Cause

`matchesPatterns` and `isExcluded` in `codeanalysis/internal/engine/orchestrator.go` call
`filepath.Match(pattern, filepath.Base(path))` and then `matchGlob(pattern, path)`, where
`path` is the absolute path from `filepath.Walk`:

- `matchGlob` handles only patterns that start with `**`: it drops the `**/` and matches
  the rest against the file name. `**/*.java` works, `**/target/**` becomes `target/**`
  and is matched against a file or directory name, which never contains `/`
- Other patterns go to `filepath.Match(pattern, path)` with the absolute path.
  `filepath.Match` has no `**` and `*` does not match `/`, so `src/main/java/**/*.java`
  never matches
- File-name-only exclude patterns (`**/*.test.ts`, `**/test_*.py`) work

There are no unit tests for `matchGlob`, `matchesPatterns` and `isExcluded`.

### Possible Solution

- Match patterns against the path relative to the analysis target, with `/` as separator
  on all platforms
- Support `**` as zero or more directories at the start, in the middle and at the end of
  a pattern: own implementation, or a library such as `github.com/bmatcuk/doublestar`
- Exclude a directory when a pattern with a trailing `/**` matches it, and skip it in the
  walk

### Open Question

`ptx-codeanalysis analyze ./src` uses the directory `./src` as the target, but the
patterns of the templates start with `src/`. Decide whether patterns are relative to the
directory of the `.ptxcode` file (project root) or to the analysis target, and document
it in `codeanalysis/README.md`.

## Acceptance Criteria

1. With each `init` template and with `defaults/sample.ptxcode`, `analyze` on a project
   with the matching layout analyzes the expected files
2. Exclude patterns `**/target/**`, `**/node_modules/**`, `**/build/**`, `**/dist/**`
   remove these directories, also with the built-in default configuration
3. File-name-only patterns (`**/*.java`, `**/*.test.ts`) keep working
4. Patterns behave the same on Linux and Windows
5. The base of relative patterns is decided (see Open Question) and documented
6. Unit tests cover the patterns of all templates, include and exclude, and a single file
   as the target

## Testing

1. `make test-go`: new unit tests for the pattern matching
2. Scenario 1 and Scenario 2 from [Current State](#current-state) with each template
3. `ptx-codeanalysis analyze` on the `codeinfo` module of this repository: files under
   `target/` are not analyzed
4. Optional: Windows
