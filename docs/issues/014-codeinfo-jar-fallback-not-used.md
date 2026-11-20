# Issue #14: code-info plugin from the JAR distribution cannot run through Portunix

## Summary

The JAR distribution of `code-info` installs only `ptx-codeinfo.jar`. Portunix looks for
the native binary `ptx-codeinfo` and does not use the `fallback` from `plugin.json`, so
the plugin cannot run through `portunix code-info`. Found as P3 in
[acceptance-013.md](../testing/acceptance-013.md).

**Status**: 📋 Open
**Type**: bug
**Priority**: medium
**Component**: codeinfo / plugin distribution
**Created**: 2026-11-20
**GitHub**: #14
**Related**: #13

## Problem Description

### Background

`codeinfo/plugin.json` declares two binaries:

```json
"plugin": {
  "type": "helper",
  "binary": "ptx-codeinfo",
  "runtime": "native",
  "fallback": {
    "binary": "ptx-codeinfo.jar",
    "runtime": "java",
    "runtime_version": ">=21"
  }
}
```

`make dist-codeinfo` builds the JAR distribution, which contains only the fallback JAR.
`make deploy-local` and the `deploy-local` skill use this distribution by default. Only
`make dist-codeinfo-native` (requires GraalVM) contains the native binary.

`codeinfo/README.md` (section "Plugin Manifest") and `docs/MODULES.md` say that the JAR is
"used where no native binary is available".

### Current State

Tested with Portunix v2.7.0+dev.3 on Linux, plugin `code-info` 1.1.0 from
`make dist-codeinfo`:

```text
$ portunix plugin health code-info
Status:     ❌ Unhealthy
Message:    Binary not found: <plugin dir>/ptx-codeinfo

$ portunix code-info loc Example.java
Error: failed to execute plugin: fork/exec <plugin dir>/ptx-codeinfo: no such file or directory
```

`portunix plugin validate` and `portunix plugin install` pass without a warning. The JAR
itself works: `java -jar ptx-codeinfo.jar loc Example.java` returns exit 0.

Problems:

- The default local deployment gives a plugin that cannot run
- The documentation describes a fallback that Portunix does not use
- `plugin validate` does not report that the declared binary is missing

### Possible Solutions

To be decided in this issue (one or more):

1. **Portunix core**: support `fallback` with `runtime: java` (run `java -jar` when the
   native binary is missing). Needs an issue in `cassandragargoyle/portunix`
2. **JAR distribution**: add a launcher `ptx-codeinfo` (shell script) and
   `ptx-codeinfo.cmd` (Windows) that run `java -jar ptx-codeinfo.jar "$@"`
3. **Local deployment**: deploy the native build by default where GraalVM is available,
   and warn when the JAR distribution is deployed
4. **Documentation**: describe the real behavior of the current Portunix in
   `codeinfo/README.md`, `docs/MODULES.md` and the `deploy-local` skill

## Acceptance Criteria

1. After `make deploy-local`, `portunix code-info loc Example.java` runs and returns
   exit 0
2. After `make deploy-local`, `portunix plugin health code-info` reports the plugin as
   healthy
3. `codeinfo/README.md` and `docs/MODULES.md` describe how the JAR distribution is run
4. The native distribution (`make dist-codeinfo-native`) still works through Portunix

## Testing

1. `make undeploy-local`, then `make deploy-local`
2. `portunix plugin health code-info`
3. From a directory outside the repository: `portunix code-info --help`,
   `portunix code-info --list-commands`, `portunix code-info loc Example.java`
4. Repeat steps 2-3 with the native distribution
   (`make dist-codeinfo-native` + `portunix plugin install dist/ptx-codeinfo`)
5. Optional: Windows
