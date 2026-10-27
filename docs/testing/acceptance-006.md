# Acceptance Protocol: Issue #006 — Standard Portunix Helper Parameters for ptx-codeinfo

**Issue**: #6 — Implement Standard Portunix Helper Parameters for ptx-codeinfo
**Branch**: `feature/006-codeinfo-standard-params`
**Tested commit**: `ca89ea1`
**Date**: 2026-10-27
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES (finding P1 should be fixed before the merge; it changes only
`CHANGELOG.md` and needs no re-test)

## Scope

The branch adds the standard Portunix helper meta flags to `ptx-codeinfo`: `-h/--help`,
`--list-commands`, `--description`, and `--version` in the format
`ptx-codeinfo version X.Y.Z` (`-v` stays as an alias). `plugin.json` becomes the single
source of the version: Maven packages it as a classpath resource, and the native image
includes it through `resource-config.json`. `--help-ai` uses the same version.

## Plan

- Clean build of the JAR and the native image (GraalVM CE 21.0.2), inspect both artifacts
- Run `make build`, `make test`, `make coverage-codeinfo`, `make test-codeinfo-native`
- Black-box cases T1-T9 from the issue on the native binary and on the JAR, with stdout,
  stderr and exit code checked separately (`test-codeinfo-native.sh` merges the streams)
- Run the black-box cases from a directory without `plugin.json`, so the version must
  come from the packaged resource and not from the working directory
- Exploratory cases: subcommand help, flag combinations, unknown options, old aliases
- Regression: build the JAR from `main` and compare `loc` output and exit codes with the
  branch JAR on all fixtures and error inputs
- Failure injection: version drift, hard-coded version, description drift, missing resource
- Build the native distribution, extract the archive, and run the binary from it

## Artifacts

| Artifact | Size | Check |
| --- | --- | --- |
| `codeinfo/target/ptx-codeinfo` | 23.8 MB | Native binary, `--version` without a JVM, `plugin.json` embedded |
| `codeinfo/target/ptx-codeinfo.jar` | 1.9 MB | Contains `plugin.json` (version `1.0.0`) and `META-INF/native-image/resource-config.json` |
| `ptx-codeinfo-1.0.0-linux-amd64.tar.gz` | - | Archive name, `plugin.json` and `--version` all show `1.0.0` |

## Cases

### Build and automated tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | Given the branch, when `make build`, then it succeeds | PASS |
| TC02 | When `make test`, then JUnit 73/73 and pytest 12/12 pass | PASS |
| TC03 | When `make coverage-codeinfo`, then the 95% line coverage gate is met (97.3%) | PASS |
| TC04 | Given `GRAALVM_HOME`, when `make build-codeinfo-native`, then the binary is built in about 13 s | PASS |
| TC05 | When `make test-codeinfo-native`, then all 21 cases match between the native binary and the JAR | PASS |

### Meta flags (issue cases, native and JAR, run outside the module directory)

| ID | Given / When / Then | Native | JAR |
| --- | --- | --- | --- |
| T1 | When `--help`, then usage on stdout that lists `loc`, empty stderr, exit 0 | PASS | PASS |
| T2 | When `-h`, then the same output as T1, exit 0 | PASS | PASS |
| T3 | Given `plugin.json` version `1.0.0`, when `--version`, then `ptx-codeinfo version 1.0.0`, exit 0 | PASS | PASS |
| T3b | When `-v`, then the same output as T3 | PASS | PASS |
| T4 | When `--help-ai`, then valid JSON with `"version": "1.0.0"` | PASS | PASS |
| T5 | When `--list-commands`, then exactly `loc`, exit 0 | PASS | PASS |
| T6 | When `--description`, then one non-empty line, exit 0 | PASS | PASS |
| T7 | When no arguments, then the same usage as T1, exit 0 | PASS | PASS |
| T8 | When `loc -v Sample.java`, then the verbose breakdown (15 code lines), no version | PASS | PASS |
| T9 | When `--bogus`, then empty stdout, `Unknown option: '--bogus'` on stderr, exit 2 | PASS | PASS |
| T10 | When T1-T9 run on both binaries, then stdout, stderr and exit code are the same | PASS | |

### Exploratory cases (native = JAR in all cases)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| X1 | When `loc --help`, then the `loc` usage, exit 0 | PASS |
| X2 | When `help loc`, then the `loc` usage, exit 0 | PASS |
| X3 | When `loc --version`, then an error was expected; it prints nothing and exits 0 | See P2 |
| X4 | When `--list-commands loc` (no file), then `loc`, exit 0 | PASS |
| X5 | When `--description --bogus`, then an error was expected; it prints the description and exits 0 | See P3 |
| X6 | When `-hv`, then usage, exit 0 | PASS |
| X7 | When `--version --help`, then exit 0 | PASS |
| X8 | When `--VERSION`, then error, exit 2 | PASS |
| X9 | When `-V` (removed picocli default), then error, exit 2 | PASS |
| X10 | When `--help-ai loc`, then JSON, exit 0 | PASS |
| X11 | When `--list-commands=true`, then `loc`, exit 0 | PASS |
| X12 | When `loc -v -h`, then the `loc` usage, exit 0 | PASS |

### Regression against `main`

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Given the `main` JAR and the branch JAR, when `loc` runs on 9 fixtures, the sample, `plugin.json` and a missing file, each without `-v`, with `-v` and with `--verbose` (36 cases), then output and exit code are the same | PASS |
| TC21 | When `loc` without a file and `loc --bogus`, then the same error and exit 2 | PASS |
| TC22 | When an unknown command `bogus`, then exit 2 as on `main`; the usage text differs only by the new options | PASS |

### Failure injection

| ID | Injection | Expected | Result |
| --- | --- | --- | --- |
| FI1 | `plugin.json` version changed to `7.7.7` | Unit tests pass; JAR and native `--version` and `--help-ai` show `7.7.7` | PASS |
| FI2 | Version provider hard-coded to `1.0.0`, `plugin.json` `7.7.7` | `testCliVersionMatchesManifest` and `testRunVersionUsesPortunixFormat` fail | PASS |
| FI3 | Description in `plugin.json` changed | `testCliDescriptionMatchesManifest` fails | PASS |
| FI4 | `plugin.json` excluded from resources in `pom.xml` | 6 tests fail with `Plugin manifest not found on classpath` | PASS |

### Distribution

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC30 | When `make dist-codeinfo-native`, then the archive name, `plugin.json` and the extracted binary's `--version` all show `1.0.0` | PASS |
| TC31 | Given the extracted binary, when the black-box cases run, then 20/22 pass (X3 and X5 as documented) | PASS |

## Coverage

| Class | Line | Branch |
| --- | --- | --- |
| `JavaLocAnalyzer` (with `CommentScan`) | 129/130 | 124/132 |
| `LinesOfCodeCommand` | 42/42 | 14/14 |
| `LocResult` | 13/13 | - |
| `Main` | 33/38 | 11/12 |
| `Main.PluginVersionProvider` | 2/2 | - |
| **Total** | **97.3%** (gate 95%) | |

Not covered in `Main`: `main()` with `System.exit`, the missing manifest branch, and the
`IOException` branch in `pluginVersion()`. FI4 checks the missing manifest branch by hand.

## Performance (Linux x64, native, median of 10 runs)

| Metric | Target | Native |
| --- | --- | --- |
| Startup (`--version`, reads `plugin.json`) | < 50 ms | 2.5 ms |
| `--list-commands` | < 50 ms | 2.2 ms |

Reading the manifest at startup has no measurable cost (2 ms in #5).

## Findings

No regressions. None of the findings blocks the merge.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Low | `codeinfo/CHANGELOG.md` is not updated: the new flags, the new `--version` format, and the removed `-V` alias are not listed; fix before the merge |
| P2 | Low | `loc --version` prints nothing and exits 0, because `loc` uses `mixinStandardHelpOptions` without a version; same on `main`, outside the scope of #6 |
| P3 | Info | Help flags ignore unknown options (`--description --bogus` exits 0); picocli behavior, same as `--version` and `--help-ai` on `main` |
| P4 | Info | `-V` (picocli default) is no longer accepted; it was not documented, and Portunix uses `--version` |
| P5 | Low | `test-codeinfo-native.sh` merges stdout and stderr, so it does not detect output on the wrong stream; the unit tests and the black-box run in this protocol cover it |

## CI Notes

- The JUnit and coverage gates run without GraalVM; the native comparison needs
  `GRAALVM_HOME` (about 15 s build) and fits a separate job (#10)
- Recommendation: keep stdout and stderr apart in `test-codeinfo-native.sh` (P5)

## Not Tested

- Discovery through the Portunix dispatcher (`portunix code-info --list-commands`),
  needs the Portunix side and changes the local Portunix installation
- Windows and macOS builds
