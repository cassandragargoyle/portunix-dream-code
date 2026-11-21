# Issue #17: code-analysis plugin from the dist package cannot find the bridge JARs

## Summary

The installed `code-analysis` plugin fails with "SonarLint bridge not found" for every
analysis with an engine enabled. The bridge lookup knows only the development layout, not
the layout of the distribution package. Also `make deploy-local` does not reinstall
plugins that are already installed and still exits with 0.

**Status**: 📋 Open
**Type**: bug
**Priority**: high
**Component**: codeanalysis / engine, Makefile
**Created**: 2026-11-21
**GitHub**: #17
**Related**: #14, #16

## Problem Description

### Current State

Build and install the plugin, then run an analysis with SonarLint or CodeScan enabled:

```bash
make dist
portunix plugin install dist/ptx-codeanalysis
~/.portunix/plugins/code-analysis/ptx-codeanalysis analyze <project> --config <project>/.ptxcode
```

Result:

```text
Error: engine error: SonarLint analysis failed: SonarLint bridge not found. Build it with 'make bridge-sonarlint' or set PTX_SONARLINT_BRIDGE environment variable
```

The same happens with `portunix code-analysis analyze`. With CodeScan only, the error is
"CodeScan bridge not found". The binary built in `codeanalysis/` works, because it runs
from the development layout.

### Cause 1: bridge lookup

`findBridgePath` in `codeanalysis/internal/engine/sonarlint.go` and
`findCodeScanBridgePath` in `codeanalysis/internal/engine/codescan.go` check only these
paths (besides the `PTX_SONARLINT_BRIDGE` and `PTX_CODESCAN_BRIDGE` environment
variables):

- `<exec dir>/bridges/<name>/target/ptx-<name>-bridge.jar`
- `<exec dir>/../bridges/<name>/target/ptx-<name>-bridge.jar`
- `bridges/<name>/target/ptx-<name>-bridge.jar`

`scripts/make-release.py` copies the JARs into the distribution as:

- `bridges/ptx-sonarlint-bridge.jar`
- `bridges/ptx-codescan-bridge.jar`

So no candidate matches in an installed plugin.

### Cause 2: `make deploy-local`

`make deploy-local` runs `portunix plugin install dist/<plugin>` without `--force`. When
the plugin is already installed, the install fails with
`plugin code-analysis already exists`. The target only prints `Warning: ... install
failed` and the exit code stays 0, so the old plugin stays installed without a clear
failure.

### Possible Solution

- Add `<exec dir>/bridges/ptx-<name>-bridge.jar` as a candidate in both lookup functions,
  and share one lookup function for both bridges
- Use `portunix plugin install --force` in `deploy-local`, and fail the target when an
  install fails

## Acceptance Criteria

1. A plugin installed from `dist/` finds both bridge JARs without environment variables
2. The development layout and the `PTX_SONARLINT_BRIDGE` and `PTX_CODESCAN_BRIDGE`
   environment variables keep working
3. `make deploy-local` reinstalls plugins that are already installed
4. `make deploy-local` exits with a non-zero code when an install fails
5. Unit tests cover the bridge lookup in the development layout and in the distribution
   layout

## Testing

1. `make test-go`: new unit tests for the bridge lookup
2. `make deploy-local` two times in a row: both runs install the plugins
3. Analysis with SonarLint and CodeScan enabled through the installed plugin, without
   `PTX_*_BRIDGE` variables
