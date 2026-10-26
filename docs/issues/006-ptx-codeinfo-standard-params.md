# Issue #006: Implement Standard Portunix Helper Parameters for ptx-codeinfo

**Status:** Open
**Priority:** Medium
**GitHub**: #6
**Component:** codeinfo
**Created:** 2026-01-20

## Summary

Implement standard Portunix helper parameter handling in ptx-codeinfo to ensure compatibility with the Portunix dispatcher and consistent user experience.

## Current State

ptx-codeinfo currently uses Picocli with non-standard parameter handling.
Verified on 2026-10-26 with the JAR and the native binary (same behavior on `main` and on
`feature/005-codeinfo-plugin`):

```bash
$ ptx-codeinfo --help
Unknown option: '--help'
Possible solutions: --help-ai
# exit code 2

$ ptx-codeinfo -h
Unknown option: '-h'
Possible solutions: --help-ai
# exit code 2

$ ptx-codeinfo --version
1.0.2
```

The `-v` flag shows version but doesn't follow Portunix conventions.

`Main` sets `mixinStandardHelpOptions = true`, but the usage lists only `-v, --version`
and `--help-ai`, so the standard `-h, --help` options are not added. A likely cause is
the own `@Option(names = {"-v", "--version"}, versionHelp = true)` in `Main`; verify
during implementation. Working alternatives today: `ptx-codeinfo help`,
`ptx-codeinfo loc --help`, and `ptx-codeinfo` without arguments.

### Inconsistent Version

The version is defined in three places with three different values:

| Source | Value | Used by |
| ------ | ----- | ------- |
| `@Command(version = ...)` in `Main.java` | `1.0.2` | `--version`, `--help-ai` |
| `codeinfo/plugin.json` | `1.0.0` | Portunix, distribution archive names (`ptx-codeinfo-1.0.0-*.tar.gz`) |
| `codeinfo/pom.xml` (parent version) | `1.0.0.2-SNAPSHOT` | Maven artifact |

`--version` must print the same version as `plugin.json`, otherwise Portunix cannot check
compatibility of the installed binary.

## Required Changes

### 1. Standard Meta Flags

Implement these flags for Portunix helper discovery and compatibility:

| Flag | Output | Purpose |
|------|--------|---------|
| `--version` | `ptx-codeinfo version 1.0.0` | Version check, compatibility |
| `--help` / `-h` | Usage information | User help |
| `--list-commands` | `loc` (or JSON array) | Command discovery |
| `--description` | `Fast code analysis tool...` | UI display |

### 2. Version Output Format

**Current:**
```
ptx-codeinfo 1.0.0
```

**Required (Portunix standard):**
```
ptx-codeinfo version 1.0.0
```

### 3. Help Flag

Add `--help` and `-h` support (Picocli default, but verify it works).

### 4. List Commands

Add `--list-commands` flag that outputs available commands:
```
loc
```

Or as JSON:
```json
["loc"]
```

### 5. Description

Add `--description` flag:
```
Fast code analysis tool powered by GraalVM Native Image
```

## Implementation Reference

See Portunix helper implementations:
- `portunix repository: src/helpers/ptx-python/main.go`
- `portunix repository: src/helpers/ptx-make/main.go`
- `portunix repository: docs/contributing/HELPER-BINARY-DEVELOPMENT.md`

### Picocli Implementation Pattern

```java
@Command(name = "ptx-codeinfo",
         mixinStandardHelpOptions = true,  // Adds --help and --version
         version = "ptx-codeinfo version 1.0.0",
         description = "Fast code analysis tool powered by GraalVM Native Image")
public class PtxCodeInfoCLI implements Callable<Integer> {

    @Option(names = "--list-commands", description = "List available commands")
    private boolean listCommands;

    @Option(names = "--description", description = "Show description")
    private boolean showDescription;

    @Override
    public Integer call() {
        if (listCommands) {
            System.out.println("loc");
            return 0;
        }
        if (showDescription) {
            System.out.println("Fast code analysis tool powered by GraalVM Native Image");
            return 0;
        }
        // Show help if no command specified
        CommandLine.usage(this, System.out);
        return 0;
    }
}
```

## Acceptance Criteria

- [ ] `ptx-codeinfo --version` outputs `ptx-codeinfo version X.Y.Z`
- [ ] `X.Y.Z` in `--version` and `--help-ai` matches `version` in `plugin.json` (one source
  of truth, for example a filtered resource generated at build time)
- [ ] `ptx-codeinfo --help` and `ptx-codeinfo -h` show usage on stdout with exit code 0
- [ ] `ptx-codeinfo --list-commands` outputs `loc`
- [ ] `ptx-codeinfo --description` outputs single-line description
- [ ] `ptx-codeinfo` (no args) shows help, not error
- [ ] All flags work in both JAR and native binary

## Files to Modify

- `codeinfo/src/main/java/org/cassandragargoyle/portunix/codeinfo/Main.java` (renamed from `PtxCodeInfoCLI` in #5)
- `codeinfo/plugin.json` and `codeinfo/pom.xml` (version source)
- `codeinfo/src/test/java/org/cassandragargoyle/portunix/codeinfo/MainTest.java`,
  `PluginManifestTest.java`
- `scripts/test-codeinfo-native.sh` (add the new flags to the native vs JAR check)

## Testing

```bash
# Test all meta flags
ptx-codeinfo --version
ptx-codeinfo --help
ptx-codeinfo -h
ptx-codeinfo --list-commands
ptx-codeinfo --description

# Test no-args behavior
ptx-codeinfo

# Verify native binary has same behavior
./target/ptx-codeinfo --version
```

### Test Cases

| # | Given | When | Then |
| - | ----- | ---- | ---- |
| T1 | JAR and native binary are built | `ptx-codeinfo --help` | Usage on stdout, lists `loc`, exit code 0 |
| T2 | JAR and native binary are built | `ptx-codeinfo -h` | Same output as T1, exit code 0 |
| T3 | `plugin.json` has `version` X.Y.Z | `ptx-codeinfo --version` | `ptx-codeinfo version X.Y.Z`, exit code 0 |
| T4 | `plugin.json` has `version` X.Y.Z | `ptx-codeinfo --help-ai` | JSON `"version": "X.Y.Z"` |
| T5 | JAR and native binary are built | `ptx-codeinfo --list-commands` | `loc`, exit code 0 |
| T6 | JAR and native binary are built | `ptx-codeinfo --description` | One line, exit code 0 |
| T7 | JAR and native binary are built | `ptx-codeinfo` (no arguments) | Usage, exit code 0 |
| T8 | JAR and native binary are built | `ptx-codeinfo loc -v Sample.java` | `-v` still means `--verbose` for `loc` (no conflict with the top-level flags) |
| T9 | JAR and native binary are built | `ptx-codeinfo --bogus` | Error on stderr, exit code 2 |
| T10 | JAR and native binary are built | T1-T9 with both | Same stdout, stderr and exit code (`make test-codeinfo-native`) |

Failure injection: change `version` in `plugin.json` only and check that a unit test fails
(T3/T4 must not pass with a hard-coded version).

## Related

- ADR-002: ptx-codeinfo CLI Application
- Issue #005: Convert codeinfo to Portunix plugin
- Portunix Helper Binary Development Guide
