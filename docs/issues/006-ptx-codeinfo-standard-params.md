# Issue #006: Implement Standard Portunix Helper Parameters for ptx-codeinfo

**Status:** Open
**Priority:** Medium
**GitHub**: #6
**Component:** codeinfo
**Created:** 2026-01-20

## Summary

Implement standard Portunix helper parameter handling in ptx-codeinfo to ensure compatibility with the Portunix dispatcher and consistent user experience.

## Current State

ptx-codeinfo currently uses Picocli with non-standard parameter handling:

```bash
$ ptx-codeinfo --help
Unknown option: '--help'
Usage: ptx-codeinfo [-v] [COMMAND]
```

The `-v` flag shows version but doesn't follow Portunix conventions.

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
- [ ] `ptx-codeinfo --help` and `ptx-codeinfo -h` show usage
- [ ] `ptx-codeinfo --list-commands` outputs `loc`
- [ ] `ptx-codeinfo --description` outputs single-line description
- [ ] `ptx-codeinfo` (no args) shows help, not error
- [ ] All flags work in both JAR and native binary

## Files to Modify

- `codeinfo/src/main/java/org/cassandragargoyle/portunix/codeinfo/Main.java` (renamed from `PtxCodeInfoCLI` in #5)

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

## Related

- ADR-002: ptx-codeinfo CLI Application
- Issue #005: Convert codeinfo to Portunix plugin
- Portunix Helper Binary Development Guide
