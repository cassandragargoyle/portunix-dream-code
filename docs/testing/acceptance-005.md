# Acceptance Protocol: Issue #005 — Convert codeinfo to Portunix Plugin

**Issue**: #5 — Convert codeinfo to Portunix Plugin (ptx-codeinfo)
**Branch**: `feature/005-codeinfo-plugin`
**Tested commit**: `a857519`; `c7ccb32` changes only `docs/issues/006-*.md` and needs no re-test
**Date**: 2026-10-26
**Tester role**: QA/Test Engineer
**STATUS**: PASS
**Approval for merge**: YES

## Scope

The branch restructures `codeinfo` as a Portunix helper plugin: package
`org.cassandragargoyle.portunix.codeinfo` with entry point `Main`, Maven artifact
`ptx-codeinfo` with the stable JAR name `ptx-codeinfo.jar`, native image in the Maven
profile `native`, module `Makefile`, `defaults/` and `testdata/`, the `plugin.json`
fallback fixed to the shipped JAR, and updated release scripts and documentation.

The refactoring must not change program behavior. Tasks 3.1-3.3 (Portunix registry,
discovery, `portunix code-info` integration) belong to the Portunix repository; tasks
3.4 (JSON output) and 4.3 (benchmarks) are moved out of #5.

## Plan

- Clean build of the JAR and the native image (GraalVM CE 21.0.2), inspect both artifacts
- Run `make build`, `make test`, `make coverage-codeinfo`, `make test-codeinfo-native`
- Black-box cases on the native binary, each compared with the JAR of the branch
- Regression: build the JAR from `main` and compare output and exit code with the
  branch JAR on all fixtures and extra inputs; any difference is a regression
- Build both distributions, extract the archives and run the artifacts from them
- Measure startup time and peak memory

## Artifacts

| Artifact | Size | Check |
| --- | --- | --- |
| `codeinfo/target/ptx-codeinfo` | 22.7 MB | ELF 64-bit x86-64, depends only on `libc` and `libz`, no JVM |
| `codeinfo/target/ptx-codeinfo.jar` | 1.8 MB | `Main-Class: org.cassandragargoyle.portunix.codeinfo.Main`, classes only in `portunix/codeinfo` |
| `ptx-codeinfo-1.0.0-linux-amd64.tar.gz` / `.zip` | 7.5 MB | `ptx-codeinfo` (mode 755) and `plugin.json`, checksums OK |
| `ptx-codeinfo-1.0.0.tar.gz` / `.zip` | 1.7 MB | `ptx-codeinfo.jar` and `plugin.json`, the fallback in `plugin.json` names this file |

## Cases

### Build and automated tests

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC01 | Given the branch, when `make build` (no GraalVM), then it succeeds | PASS |
| TC02 | When `make test`, then JUnit 61/61 and pytest 12/12 pass | PASS |
| TC03 | When `make coverage-codeinfo`, then the 95% line coverage gate is met (98.5%) | PASS |
| TC04 | Given `GRAALVM_HOME`, when `make build-codeinfo-native` (`-Pnative`), then `target/ptx-codeinfo` is built in about 15 s | PASS |
| TC05 | When `make test-codeinfo-native`, then the native binary matches the JAR | PASS |
| TC06 | When `make -C codeinfo help`, then the module targets are listed | PASS |
| TC07 | When `make loc-file FILE=codeinfo/testdata/Sample.java`, then the new main class runs (26/15/6/5) | PASS |

### Plugin structure

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC10 | Given the JAR, then `Main-Class` is `org.cassandragargoyle.portunix.codeinfo.Main` and no `dream` class is packaged | PASS |
| TC11 | Given `plugin.json`, then the fallback `ptx-codeinfo.jar` matches `<finalName>` (`PluginManifestTest`) | PASS |
| TC12 | Given `plugin.json`, then all CLI subcommands are listed (`PluginManifestTest`) | PASS |
| TC13 | When `make dist-codeinfo-native`, then the archive contains the native binary and `plugin.json`, and it runs after extraction | PASS |
| TC14 | When `make dist-codeinfo`, then the archive contains `ptx-codeinfo.jar` and `plugin.json`, and `java -jar` runs after extraction | PASS |

### Line counting and errors (native = JAR = `main` in all cases)

| ID | Given / When / Then | Result |
| --- | --- | --- |
| TC20 | Given `testdata/Sample.java`, when `loc -v`, then 26 total, 15 code, 6 comment, 5 empty | PASS |
| TC21 | Given a path with spaces, then the file is analyzed | PASS |
| TC22 | Given CRLF line endings, then 5/3/1/1 | PASS |
| TC23 | Given a generated 5000 line file, then 3335 code and 1667 comment lines as computed | PASS |
| TC24 | Given `Upper.JAVA`, then it is analyzed, exit 0 | PASS |
| TC25 | Given a Latin-1 file, a file without read permission, a directory, or `plugin.json`, then the matching error, exit 2 | PASS |
| TC26 | When `loc` without a file, `loc --bogus`, or an unknown command, then usage error, exit 2 | PASS |
| TC27 | When `--version` and `--help-ai`, then the same output as `main` | PASS |
| TC28 | Given the `main` JAR and the branch JAR, when all 9 fixtures, the sample and the extra inputs run with and without `-v`, then there are 0 differences | PASS |

## Coverage

| Class | Line | Branch |
| --- | --- | --- |
| `JavaLocAnalyzer` (with `CommentScan`) | 129/130 | 124/132 |
| `LinesOfCodeCommand` | 42/42 | 14/14 |
| `LocResult` | 13/13 | - |
| `Main` | 16/18 (`main()` with `System.exit`) | 2/2 |
| **Total** | **98.5%** (gate 95%) | |

## Performance (Linux x64, GraalVM CE 21.0.2, median of 10-20 runs)

| Metric | Target | Native | JAR | Note |
| --- | --- | --- | --- | --- |
| Startup (`--version`) | < 50 ms | 2 ms | 90 ms | PASS |
| `Sample.java` | < 100 ms | about 2 ms | 130 ms | PASS |
| 5000 LOC | < 500 ms | 51 ms | 190 ms | PASS |
| Memory `Sample.java` | < 30 MB | 19.2 MB | 69 MB | PASS |
| Memory 5000 LOC | < 30 MB | 39.8 MB | 147 MB | Above target, documented |
| Binary size | < 20 MB | 22.7 MB | - | Above target, documented |

Values match `codeinfo/README.md`; the refactoring has no performance impact.

## Findings

No regressions. None of the findings blocks the merge.

| ID | Severity | Finding |
| --- | --- | --- |
| P1 | Known | Root `--help` and `-h` are rejected with exit 2 (same on `main`); tracked in #6 with test cases T1-T10 |
| P2 | Medium | The version has three values: `--version` `1.0.2`, `plugin.json` and archive names `1.0.0`, `pom.xml` `1.0.0.2-SNAPSHOT`; tracked in #6 |
| P3 | Low | The native distribution does not contain the fallback JAR declared in `plugin.json`; by design and documented in the README, Portunix behavior not verified (see [Not Tested](#not-tested)) |
| P4 | Low | Release archives do not contain `README.md` because the script skips `.md` files (same on `main`) |
| P5 | Low | Without `GRAALVM_HOME` and with a non-GraalVM `JAVA_HOME`, the native build fails with `'gu' tool was not found`; documented in the README |

## Not Tested

- Installation into Portunix (`make deploy-local`) and dispatch through
  `portunix code-info loc ...` (changes the local Portunix installation)
- Windows and macOS builds

## CI Notes

- Pull request checks (#10): `make coverage-codeinfo` and `make build`; both work without GraalVM
- Nightly or release job with GraalVM 21 and `GRAALVM_HOME`: `make build-codeinfo-native`
  and `make test-codeinfo-native` (CPU heavy, about 15 s)
- Track the native binary size as a metric (22.7 MB, target 20 MB)
