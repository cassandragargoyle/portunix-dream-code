# Acceptance Protocol: Issue #013 — Plugin Manifest Placeholder Commands

**Issue**: #13 — Plugin manifest lists a placeholder instead of real commands
**Branch**: `fix/013-plugin-manifest-commands`
**Tested commit**: working tree on top of `c499f87` (first test), re-test of the finding
fixes on `c70258c`
**Date**: 2026-11-20
**Tester role**: QA/Test Engineer
**Environment**: Linux, OpenJDK 21.0.10, Maven 3.9.12, Portunix v2.7.0+dev.3
**STATUS**: PASS
**Approval for merge**: YES; no blocking findings; P1 and P2 are fixed in `c70258c` and
re-tested (see [Re-test of `c70258c`](#re-test-of-c70258c)); P3 is an existing defect
outside #13 and needs a separate issue

## Scope

`codeinfo/plugin.json` lists the real subcommand `loc` with a description instead of the
placeholder entry `code-info` with `subcommands: ["loc"]`. `PluginManifestTest` replaces
the check of the `subcommands` array with five checks: all CLI commands are listed, only
CLI commands are listed, no placeholder entry, descriptions match the CLI, examples start
with `portunix code-info <command>`.

## Plan

- Static checks: manifest content against `ptx-codeinfo --help` and `--list-commands`
  (AC1-AC4), the examples run
- Unit tests and the coverage gate of `codeinfo`
- Failure injection: one change of the manifest at a time, `make test-codeinfo`, then the
  manifest is restored (checked with SHA-256)
- Robustness: valid manifest variants must not fail the tests
- Deployment: baseline of the installed plugin, `make dist-codeinfo`, `portunix plugin
  validate`, reinstall, `portunix plugin list --json` from a directory outside the
  repository (AC5, AC6)
- Regression: `make build`, `make test`

## Cases

### Static checks and acceptance criteria

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | Given `codeinfo/plugin.json`, then it is valid JSON and has no command named `code-info` or `ptx-codeinfo` (AC1) | PASS |
| TC02 | Given `ptx-codeinfo --help`, then every manifest command exists there, and the manifest description equals the first help line without the ending period (AC2) | PASS, `loc`: "Count lines of code in a Java source file" |
| TC03 | Given `ptx-codeinfo --list-commands` (`loc`), then every command is in the manifest (AC3) | PASS |
| TC04 | Given the examples, then they start with `portunix code-info loc`, and the same arguments run with the JAR with exit 0 (AC4) | PASS |
| TC05 | Given the JAR and `dist/ptx-codeinfo/`, then the packaged `plugin.json` is identical to the source | PASS |

### Unit tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | `make test-codeinfo` | PASS, 80/80 (`PluginManifestTest` 9/9) |
| TC11 | `make coverage-codeinfo` (gate 95%) | PASS, "All coverage checks have been met" |

### Failure injection

Each case changes `codeinfo/plugin.json` and runs `make test-codeinfo`. Expected: exit ≠ 0
and the named test fails.

| ID | Injection | Failing tests | Result |
| --- | --- | --- | --- |
| FI0 | None (control) | none, exit 0 | PASS |
| FI1 | Placeholder entry `code-info` added | `HasNoPlaceholderCommand`, `ListsOnlyRealCommands`, `DescriptionsMatchCli` | PASS (see P1) |
| FI2 | Placeholder entry `ptx-codeinfo` added | same as FI1 | PASS (see P1) |
| FI3 | Description of `loc` removed | `DescriptionsMatchCli` | PASS |
| FI4 | Description shortened | `DescriptionsMatchCli` | PASS |
| FI5 | Description with ending period | `DescriptionsMatchCli` | PASS |
| FI6 | Example `portunix codeinfo loc …` (wrong plugin name) | `ExamplesStartWithCommand` | PASS |
| FI7 | Example `portunix code-info locx …` (prefix without word boundary) | `ExamplesStartWithCommand` | PASS |
| FI8 | Empty `commands` | `ListsAllCommands` | PASS |
| FI9 | Unknown command `metrics` added | `ListsOnlyRealCommands`, `DescriptionsMatchCli` | PASS (see P1) |
| FI10 | Name changed to `LOC` | 4 tests | PASS |

### Robustness (valid manifests)

| ID | Variant | Expected | Result |
| --- | --- | --- | --- |
| FI11 | Whole manifest on one line | pass | PASS |
| FI12 | `parameters` with nested objects after `examples` | pass | PASS |
| FI13 | `commands` as the first key, not the last | pass | PASS |
| FI14 | Example is the bare command `portunix code-info loc` | pass | PASS |
| FI15 | `parameters` as the first key of the command entry | pass | **FAIL**, 4 tests fail, see P2 |

### Deployment

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Baseline: installed `code-info` 1.1.0 (JAR build of 2026-10-27), `plugin list --json` shows the placeholder `code-info` with `subcommands: ["loc"]` | Defect reproduced |
| TC21 | `portunix plugin validate` on the new `dist/ptx-codeinfo` (AC6) | PASS, no warning |
| TC22 | `portunix plugin validate` on the old installed plugin | Also passes, see [Not Tested](#not-tested) |
| TC23 | `portunix plugin install dist/ptx-codeinfo` over the installed plugin | Refused: "plugin code-info already exists"; nothing changed. Reinstalled with `plugin uninstall code-info -f` + `plugin install` |
| TC24 | After reinstall, `portunix plugin list --json` from a directory outside the repository shows `loc` with its description and both examples (AC5) | PASS |
| TC25 | `portunix plugin info code-info` shows `loc: Count lines of code in a Java source file` | PASS |
| TC26 | The installed `plugin.json` is identical to the source | PASS |

### Regression

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC30 | `make build` | PASS |
| TC31 | `make test` | PASS, JUnit 80/80, bridges 3/3 and 9/9, pytest 12/12 |
| TC32 | After all cases, `git status` shows only the two changed files of the branch | PASS |

## Coverage

| Acceptance criterion | Automated (JUnit) | Manual |
| --- | --- | --- |
| AC1 no placeholder | `testManifestHasNoPlaceholderCommand` | TC01 |
| AC2 commands exist, have a description | `testManifestListsOnlyRealCommands`, `testCommandDescriptionsMatchCli` | TC02 |
| AC3 all commands listed | `testManifestListsAllCommands` | TC03 |
| AC4 example prefix | `testCommandExamplesStartWithCommand` | TC04 |
| AC5 `plugin list --json` | - | TC24 |
| AC6 `plugin validate` | - | TC21 (see Not Tested) |

The failure injection shows that each automated check detects the defect it is written
for. The `codeinfo` coverage gate is met; the change adds no production code.

## Findings

No blocking findings.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Low | `testCommandDescriptionsMatchCli` ends with a `NullPointerException` ("Cannot invoke getCommandSpec() because the return value of Map.get(Object) is null") when the manifest lists a command that the CLI does not have (FI1, FI2, FI9). The build fails as it should, and `testManifestListsOnlyRealCommands` gives the clear message, but the NPE is noise in the report. Suggestion: assert that the subcommand exists (`assertNotNull(…, "Command not provided by the CLI: " + name)`) before reading its description. **Fixed** in `c70258c` (R02) |
| P2 | Medium | `commandName()` and the description regex take the first `"name"` / `"description"` anywhere in the entry, also inside nested `parameters`. A valid entry with `parameters` before `name` (FI15) fails 4 tests: the parameter `sourceFile` is taken as the command name. JSON key order is not significant, and the issue plans to add `parameters` (cassandragargoyle/portunix#257). Suggestion: read only the top-level keys of the entry (depth 1), or use a JSON parser in the tests. **Fixed** in `c70258c`: the tests read the manifest with a string-aware JSON reader that uses only top-level keys (R03-R05) |
| P3 | Medium | **Existing defect, outside #13.** The installed plugin is the JAR build (`ptx-codeinfo.jar`), and Portunix does not use the `fallback` from the manifest: `portunix plugin health code-info` reports "Binary not found: …/ptx-codeinfo", and `portunix code-info …` fails with "fork/exec …/ptx-codeinfo: no such file or directory". Same before and after the reinstall. So `portunix code-info --help` from the issue Testing step 3 cannot run with a JAR deployment; the CLI was checked with `java -jar` instead. Suggestion: separate issue (dream-code `deploy-local` with the native build, or Portunix core support for `fallback`) |
| P4 | Info | `portunix plugin install` refuses an installed plugin, and `make deploy-local` only prints a warning with exit 0 (as documented in the `deploy-local` skill). A reinstall needs `make undeploy-local` or `portunix plugin uninstall` first |

## Re-test of `c70258c`

The commit contains the fix of #13 together with the fixes of P1 and P2. Only
`PluginManifestTest.java` changed compared with the first test; `plugin.json` and the
production code are the same, so the deployed plugin from TC24 is still valid. The failure
injection (FI0-FI14) was repeated and extended with FI15-FI19.

| ID | Given / When / Then | Result |
| --- | --- | --- |
| R01 | Given `c70258c`, then the working tree of `codeinfo` is identical to the commit, no production code changed since `c499f87`, and the deployed `plugin.json` equals the committed one | PASS |
| R02 | FI1, FI2, FI9 (command that the CLI does not have): the tests fail with "Command not provided by the CLI: …", no `NullPointerException` (P1) | PASS, `Errors: 0` in all 20 cases |
| R03 | FI15 (`parameters` as the first key of the entry) passes (P2) | PASS |
| R04 | FI16 (example with `{[}]` and `\"name\":` inside the string) passes | PASS |
| R05 | FI18 (description missing, `parameters` has a nested `description` with the same text) fails with "Command without description"; FI19 (nested `name: loc` in a placeholder entry) fails in 4 tests including `HasNoPlaceholderCommand` | PASS |
| R06 | FI17 (example `name`, a value equal to a key name) fails in `ExamplesStartWithCommand` | PASS |
| R07 | FI0-FI14 give the same failing tests as in the first test | PASS |
| R08 | `make coverage-codeinfo`, `make build`, `make test` | PASS, JUnit 80/80, bridges 3/3 and 9/9, pytest 12/12, coverage gate met |
| R09 | `portunix plugin list --json` from outside the repository shows `loc` with its description | PASS |
| R10 | After all cases, the working tree is clean apart from this protocol | PASS |

## CI Notes

- The new checks are JUnit tests in `codeinfo` and run in the existing CI job; no
  workflow change is needed
- AC5 and AC6 depend on a local Portunix and are manual

## Not Tested

- `portunix plugin validate` warning for placeholder entries: Portunix v2.7.0+dev.3 does
  not have the check from cassandragargoyle/portunix#257 yet; the old manifest also passes
  (TC22). AC6 is met for the current Portunix; re-check when #257 is released
- `portunix code-info --help` via the dispatcher, because of P3
- Functions panel of the VS Code extension (optional step 5)
- Native image build, Windows, macOS

## Rollback of the Test Deployment

The plugin directory from before the test is saved in the session scratchpad
(`installed-code-info-backup`). The content differs only in `plugin.json` and the rebuilt
JAR. To go back: `portunix plugin uninstall code-info -f`, then `portunix plugin install`
from the `main` build (`git checkout main && make dist-codeinfo`).
