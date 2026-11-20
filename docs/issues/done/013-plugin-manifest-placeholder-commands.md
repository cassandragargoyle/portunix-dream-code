# Issue #13: Plugin manifest lists a placeholder instead of real commands

## Summary

The `commands` list in `codeinfo/plugin.json` has one placeholder entry named after the
plugin (`code-info`) instead of one entry per subcommand. Portunix clients read the
commands from the manifest, so they cannot show what the plugin offers. Replace the
placeholder with the real subcommand `loc`, with a description, parameters and examples.

**Status**: ✅ Implemented
**Type**: bug
**Priority**: medium
**Component**: codeinfo / plugin manifest
**Created**: 2026-11-20
**Closed**: 2026-11-20
**Acceptance**: [acceptance-013.md](../../testing/acceptance-013.md)
**GitHub**: #13
**Related**: cassandragargoyle/portunix#257, cassandragargoyle/portunix-vscode#145

## Problem Description

### Background

- `commands` in a plugin manifest lists one entry per subcommand that a user calls as
  `portunix <plugin> <name>`. Each entry has a description, and optionally parameters and
  examples. There is no entry for the binary or the plugin itself. A plugin without
  subcommands has an empty list. This rule is being defined in Portunix core issue
  `cassandragargoyle/portunix#257`: `--help-ai` for plugin commands will be built from the
  manifest, and `portunix plugin validate` will warn about placeholder entries.
- The fields of one command (core type `PluginCommand`) are `name`, `description`,
  `subcommands`, `parameters` and `examples`.
- Clients read the commands with `portunix plugin list --json`. The Functions panel of the
  VS Code extension (`cassandragargoyle/portunix-vscode#145`) shows them. With a
  placeholder entry the panel shows only bare subcommand names without descriptions.

### Current State

`codeinfo/plugin.json` (version 1.1.0):

```json
"commands": [
  {
    "name": "code-info",
    "description": "Code analysis commands",
    "subcommands": ["loc"],
    "examples": [
      "portunix code-info loc Example.java",
      "portunix code-info loc Example.java --verbose"
    ]
  }
]
```

`portunix plugin list --json` (Portunix v2.7.0+dev.3) returns the same placeholder.

Problems:

- `code-info` is the plugin name, not a command. `portunix code-info code-info` does not
  exist
- The real command `loc` appears only as a name, without a description
- The description "Code analysis commands" describes the group, not a command

The examples are correct: they already start with `portunix code-info loc`.

### Real Commands

`ptx-codeinfo --help` and `ptx-codeinfo --list-commands` list one command (plus the
generic `help` command of the CLI framework, which does not belong in the manifest):

| Command | Description from `--help` | Parameters |
| ------- | ------------------------- | ---------- |
| `loc` | Count lines of code in a Java source file | `<sourceFile>` (required), `--verbose` / `-v` |

### Expected State

```json
"commands": [
  {
    "name": "loc",
    "description": "Count lines of code in a Java source file",
    "examples": [
      "portunix code-info loc Example.java",
      "portunix code-info loc Example.java --verbose"
    ]
  }
]
```

Parameters may be added in the format that `cassandragargoyle/portunix#257` defines.

The sibling manifest `codeanalysis/plugin.json` lists `analyze`, which is a real
command, so it is not part of this issue.

## Acceptance Criteria

- [x] `codeinfo/plugin.json` has no placeholder entry: no command named `code-info` or
  `ptx-codeinfo`
- [x] Every listed command exists in `ptx-codeinfo --help` and has a description
- [x] Every command of `ptx-codeinfo --list-commands` is listed
- [x] Examples start with `portunix code-info <command>`
- [x] After reinstall of the plugin, `portunix plugin list --json` shows the real commands
- [x] `portunix plugin validate` passes without warnings about the commands (Portunix
  v2.7.0+dev.3; this version does not check placeholder entries yet, re-check when
  cassandragargoyle/portunix#257 is released)

## Testing

1. Reinstall the plugin from the repository build
2. From a directory outside the repository, run `portunix plugin list --json` and check
   the `commands` of `code-info`
3. Compare the list with `portunix code-info --help` and `ptx-codeinfo --list-commands`
4. Run `portunix plugin validate` on the plugin directory
5. Optional: open the Functions panel in the VS Code extension and check that `loc` has a
   description
