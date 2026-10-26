# #008 - Implement --help-ai Flag for Dream Code CLIs

**Status**: ✅ Implemented
**Closed**: 2026-10-26
**Priority**: Medium
**GitHub**: #8
**Type**: Enhancement
**Labels**: enhancement, cli, ai-integration

## Context

All Portunix ecosystem CLIs should provide a `--help-ai` flag that outputs
machine-readable JSON describing the tool's commands, parameters, and capabilities.
This enables AI assistants (Claude Code, MCP clients) to discover and use the CLI
programmatically without parsing human-readable help text.

The pattern is already implemented in sibling Java/Picocli projects:
- **portunix-plugins/fulltext** — `Main.java` → `@Option(names = {"--help-ai"})` + `printHelpAI()`
- **portunix-plugins/text-extractor** — `Main.java` → manual arg parsing + `printHelpAI()`

## Scope

This issue covers three CLI tools in portunix-dream-code:

1. **ptx-codeinfo** (`codeinfo/src/main/java/.../PtxCodeInfoCLI.java`)
2. **ptx-sonarlint-bridge** (`codeanalysis/bridges/sonarlint/src/main/java/.../Main.java`)
3. **ptx-codescan-bridge** (`codeanalysis/bridges/codescan/src/main/java/.../Main.java`)

## Requirements

### 1. ptx-codeinfo (Picocli)

Add `@Option` annotation and handle before `execute()`:

```java
@Option(names = {"--help-ai"}, description = "Show machine-readable help in JSON format")
private boolean helpAI = false;
```

JSON output:

```json
{
  "tool": "ptx-codeinfo",
  "version": "1.0.2",
  "description": "Fast code analysis tool powered by GraalVM",
  "commands": [
    {
      "name": "loc",
      "brief": "Count lines of code in a Java source file",
      "parameters": [
        {"name": "file", "type": "string", "required": true, "description": "Java source file to analyze"},
        {"name": "--verbose", "type": "bool", "required": false, "description": "Show detailed breakdown"}
      ]
    }
  ]
}
```

### 2. ptx-sonarlint-bridge (Picocli)

```json
{
  "tool": "ptx-sonarlint-bridge",
  "version": "1.0.0",
  "description": "SonarLint bridge for ptx-codeanalysis",
  "commands": [
    {
      "name": "analyze",
      "brief": "Run SonarLint analysis on specified files",
      "description": "Reads JSON from stdin with files, languages, and rules config. Returns JSON with issues and stats."
    },
    {
      "name": "rules",
      "brief": "List available SonarLint rules",
      "parameters": [
        {"name": "--language", "type": "string", "required": false, "description": "Filter by language"}
      ]
    }
  ]
}
```

### 3. ptx-codescan-bridge (Picocli)

```json
{
  "tool": "ptx-codescan-bridge",
  "version": "1.0.0",
  "description": "CodeScan bridge for code validation - annotations and patterns",
  "commands": [
    {
      "name": "analyze",
      "brief": "Run CodeScan analysis on specified files",
      "description": "Reads JSON from stdin with files, annotations, patterns, and severity mapping. Returns JSON with issues and stats."
    }
  ]
}
```

### 4. Implementation Pattern (Picocli)

For all three CLIs, follow the fulltext plugin pattern:

1. Add `@Option(names = {"--help-ai"})` boolean field
2. In `main()`, call `cmd.parseArgs(args)` first
3. Check the flag and call `printHelpAI()` before `cmd.execute(args)`
4. Add a static `printHelpAI()` method with the JSON string

```java
public static void main(String[] args) {
    Main main = new Main();
    CommandLine cmd = new CommandLine(main);
    cmd.parseArgs(args);

    if (main.helpAI) {
        printHelpAI();
        System.exit(0);
    }

    int exitCode = cmd.execute(args);
    System.exit(exitCode);
}
```

## Acceptance Criteria

- [x] `ptx-codeinfo --help-ai` outputs valid JSON
- [x] `ptx-sonarlint-bridge --help-ai` outputs valid JSON
- [x] `ptx-codescan-bridge --help-ai` outputs valid JSON
- [x] Each JSON contains `tool`, `version`, `description`, `commands` fields
- [x] Exit code is 0 for all three tools
- [x] No other output is mixed into the JSON

## References

- portunix-plugins/fulltext `Main.java` — Java/Picocli reference (same pattern)

---

**Created**: 2026-03-26
**Author**: Architect
