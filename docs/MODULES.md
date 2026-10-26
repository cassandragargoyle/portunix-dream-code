# Modules

Each module is a Portunix helper plugin: it has a `plugin.json` manifest, its own
`Makefile`, `defaults/` for configuration templates and `testdata/` for sample sources.
Portunix dispatches `portunix <plugin-name> <args>` to the plugin binary.
The root `Makefile` delegates to the module Makefiles.

| Module | Plugin name | Binary | Languages | Details |
| ------ | ----------- | ------ | --------- | ------- |
| `codeanalysis/` | `code-analysis` | `ptx-codeanalysis` | Go, Java bridges | [README](../codeanalysis/README.md) |
| `codeinfo/` | `code-info` | `ptx-codeinfo` | Java (GraalVM Native Image) | [README](../codeinfo/README.md) |

## codeanalysis

Static code analysis with SonarLint and CodeScan. A Go CLI reads the `.ptxcode`
configuration and runs the Java bridges in `bridges/`.

- Build: `make build-codeanalysis` (or `make -C codeanalysis build`)
- Test: `make test-codeanalysis`

## codeinfo (experimental)

Code information and metrics, currently the `loc` command for Java files. One Java
application (Picocli, JavaParser) built as a JAR and optionally as a GraalVM native
image; there is no Go wrapper (see issue #5, option A).

- Package: `org.cassandragargoyle.portunix.codeinfo`, entry point `Main`
- Build: `make build-codeinfo` (JAR, `target/ptx-codeinfo.jar`) or
  `make build-codeinfo-native` (Maven profile `native`, `target/ptx-codeinfo`)
- Test: `make test-codeinfo`, `make coverage-codeinfo`, `make test-codeinfo-native`
- `plugin.json` declares the native binary and the JAR as fallback
