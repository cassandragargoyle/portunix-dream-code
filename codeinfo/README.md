# ptx-codeinfo - Fast Code Analysis CLI

> **⚠️ Experimental Project**: This is an experimental project exploring GraalVM Native Image capabilities for building high-performance CLI tools. APIs and features may change without notice.

A high-performance code analysis tool powered by GraalVM Native Image, Picocli, and JavaParser.

## Features

- **Fast Startup**: About 2 ms startup with GraalVM Native Image
- **Lines of Code Counter**: Accurate LOC counting for Java source files
- **Zero Dependencies**: Native binary with no JVM required
- **Cross-Platform**: Linux x64 (tested), Windows x64 (see [Platform Requirements](#platform-requirements))

## Installation

### From Binary (Recommended)

Download the latest binary for your platform from the releases page and add it to your PATH.

### As a Portunix Plugin

```bash
# Build the native distribution and install it into the local Portunix
make dist-codeinfo-native
make deploy-local
```

### From Source

Requirements:

- Java 21 and Maven 3.8+ (JAR build)
- GraalVM for JDK 21 (GraalVM 23.1) or later, only for the native image
- GNU Make

```bash
# Build JAR (from the repository root)
make build-codeinfo

# Build native image (requires GRAALVM_HOME)
make build-codeinfo-native
```

### Installing GraalVM on Linux

The recommended way to install GraalVM is using Portunix:

```bash
# Install GraalVM (latest)
portunix install graalvm

# Install specific version
portunix install graalvm --variant=25    # Latest JDK 25
portunix install graalvm --variant=21    # LTS

# Verify installation
java -version
native-image --version
```

Alternative: Using SDKMAN:

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install GraalVM
sdk install java 21.0.2-graalce
sdk default java 21.0.2-graalce
```

`make build-codeinfo-native` needs `GRAALVM_HOME` (or a GraalVM `native-image` on `PATH`):

```bash
export GRAALVM_HOME=/path/to/graalvm
make build-codeinfo-native
```

For detailed GraalVM information, see the [GraalVM manifest](https://github.com/CassandraGargoyle/portunix-architecture/blob/main/docs/manifests/manifest-graalvm.md).

## Usage

### Lines of Code Command

Count lines of code in a Java source file:

```bash
# Simple output
ptx-codeinfo loc Example.java
# Output: 5 lines of code in Example.java

# Verbose output with detailed breakdown
ptx-codeinfo loc Example.java --verbose
# Output:
# Analyzed: Example.java
# Total lines: 10
# Code lines: 5
# Comment lines: 3
# Empty lines: 2
```

Run `ptx-codeinfo loc --help` for all options, examples and exit codes.
Error and warning prefixes are colored when the output is a terminal; set
`NO_COLOR=1` or `-Dpicocli.ansi=false` (JAR) to turn colors off.

### Exit Codes

| Code | Meaning |
| ---- | ------- |
| 0 | Success (also for files with syntax errors, see below) |
| 1 | Unexpected error during analysis |
| 2 | Invalid input: file not found, not a regular file, not readable, not `.java`, not valid UTF-8, missing argument, or unknown option |

### Counting Rules

The LOC counter follows these rules:

- **Count**: Lines with executable code
- **Count**: Lines with both code and comments (e.g., `int x = 1; // comment`)
- **Skip**: Empty lines (whitespace only: space, tab and form feed, as in the Java
  language; other Unicode spaces such as U+3000 are code)
- **Skip**: Comment-only lines (single-line `//` or multi-line `/* */`)

Comments are detected by the Java lexer, so comment markers inside string literals
and text blocks are not treated as comments. A line with several comments and no code
(e.g. `/* a */ /* b */`) is a comment line.

Unicode escapes are decoded before comments are detected, as in `javac`, so
`\u002F\u002F text` is a comment line and a line with only `\u0020` is an empty line.
An invalid escape such as `\u00G1` is a syntax error, also inside comments. Unicode
spaces that are not Java whitespace (such as U+3000) are a syntax error outside comments
and literals, as in `javac`. An unclosed `/*` makes all following lines comment lines.

Files are read as UTF-8. The `.java` extension is matched case insensitively. Files with
syntax errors are still counted: a warning is printed to stderr and the counts are
based on lexical analysis only.

## Platform Requirements

| Platform | Status | Native build requirements |
| -------- | ------ | ------------------------- |
| Linux x64 | Tested | GraalVM, `gcc`, `glibc` and `zlib` development headers |
| Windows x64 | Not tested yet | GraalVM, Visual Studio 2022 Build Tools with "Desktop development with C++"; run the build from the "x64 Native Tools Command Prompt" |
| macOS | Not supported yet | - |

The native binary depends only on the system C library. The JAR runs on any
platform with Java 21.

## Troubleshooting

| Problem | Cause and fix |
| ------- | ------------- |
| `Error: File must be a Java source file (.java)` | Only `.java` files are analyzed; pass a single Java file, directories are not supported yet |
| `Error: File is not valid UTF-8 text` | The file uses another encoding; convert it, e.g. `iconv -f windows-1250 -t utf-8 In.java > Out.java` |
| `Warning: ... has syntax errors` | The file does not compile; counts are still correct for comments and empty lines because they come from the lexer |
| `native-image` not found during build | Set `GRAALVM_HOME` to the GraalVM directory or add its `bin/` to `PATH` |
| Native build fails on Windows with linker errors | Start the build from the "x64 Native Tools Command Prompt" so the MSVC tools are on `PATH` |
| Escape codes like `[1;31m` in the output | The terminal does not support colors; set `NO_COLOR=1` |

## Development

### Project Structure

```text
codeinfo/
├── src/main/java/org/cassandragargoyle/portunix/codeinfo/
│   ├── Main.java                     # Main entry point
│   ├── commands/
│   │   └── LinesOfCodeCommand.java   # LOC command implementation
│   └── analyzers/
│       ├── JavaLocAnalyzer.java      # LOC analysis logic
│       └── LocResult.java            # Result container
├── src/main/resources/META-INF/native-image/  # GraalVM Native Image configuration
├── src/test/java/                    # Unit tests
├── src/test/resources/fixtures/      # Unit test fixtures
├── defaults/                         # Default configuration templates (none yet)
├── testdata/                         # Sample sources for manual and integration tests
├── plugin.json                       # Portunix plugin manifest
├── pom.xml                           # Maven build (profiles: default JAR, native)
└── Makefile                          # Module build targets
```

### Plugin Manifest

`plugin.json` registers `ptx-codeinfo` as a Portunix helper plugin named `code-info`.
Portunix dispatches `portunix code-info <args>` to the plugin binary:

- `binary` - the native image `ptx-codeinfo` (primary)
- `fallback` - `ptx-codeinfo.jar`, used where no native binary is available (requires Java 21+)

The JAR distribution (`make dist-codeinfo`) contains only the fallback JAR, the native
distribution (`make dist-codeinfo-native`) only the native binary.

### Running Tests

Run from the repository root (or the same targets without the `codeinfo` suffix with
`make -C codeinfo`, see `make -C codeinfo help`):

```bash
# Unit tests (analyzer and loc command)
make test-codeinfo

# Unit tests with coverage report and 95% line coverage gate
make coverage-codeinfo
# Report: codeinfo/target/site/jacoco/index.html

# Compare native binary output with the JAR on all fixtures
make build-codeinfo-native
make test-codeinfo-native
```

### Building

```bash
# Build JAR only
make build-codeinfo

# Build native image (requires GRAALVM_HOME)
make build-codeinfo-native
```

The JAR is the default Maven build. The native image is built by the Maven profile
`native` (`mvn package -Pnative`), which the `build-codeinfo-native` target activates.

### Running from JAR

```bash
java -jar codeinfo/target/ptx-codeinfo.jar loc codeinfo/testdata/Sample.java
```

## Architecture

This tool uses:

- **Picocli 4.7.7** for CLI parsing and command structure (excellent GraalVM support)
- **JavaParser 3.28.2** for Java source code parsing and AST analysis
- **GraalVM Community Edition** for ahead-of-time compilation to native binary

Flow of `ptx-codeinfo loc <file>`:

1. `Main` parses arguments with Picocli and dispatches to the `loc` subcommand
2. `LinesOfCodeCommand` validates the file and prints the result or an error
3. `JavaLocAnalyzer` parses the file with JavaParser (`LanguageLevel.RAW`, no token
   list), marks the columns covered by comments, and classifies each line as code,
   comment or empty
4. `LocResult` holds the counts and the syntax error flag

See [ADR-002](../docs/adr/002-ptx-codeinfo-cli-application.md) for architectural decisions.

See [GRAALVM-COMPATIBILITY.md](GRAALVM-COMPATIBILITY.md) for detailed information about GraalVM Native Image compatibility and build instructions.

## Performance

Measured on Linux x64 with GraalVM CE 21.0.2 (median of 20 runs, peak RSS).
The JAR runs on Temurin 21 for comparison.

| Metric | Target | Native | JAR |
| ------ | ------ | ------ | --- |
| Startup Time (`--version`) | < 50ms | 2.5 ms | 112 ms |
| Single File (100 LOC) | < 100ms | 2.5 ms | 171 ms |
| Large File (5000 LOC) | < 500ms | 58 ms | 209 ms |
| Binary Size | < 20MB | 22.7 MB | - |
| Memory Usage (100 LOC) | < 30MB | 19.5 MB | 77 MB |
| Memory Usage (5000 LOC) | < 30MB | 35.4 MB | 202 MB |

Memory grows with file size because the whole syntax tree is kept in memory. Two
settings keep it low: the parser does not store the token list, and the native image
limits the young generation to 8 MB (`-R:MaxNewSize=8m` in `native-image.properties`).
Before these settings a 5000 LOC file needed 110 MB and 132 ms.

Binary size and memory for large files are still above target. GraalVM 23.1 has no
size optimization level; `-Os` in newer GraalVM releases or UPX compression may close
the size gap.

## Future Enhancements

- Cyclomatic complexity analysis
- Import dependency analysis
- Batch processing for directories
- Multiple output formats (JSON, CSV, XML)
- Multi-language support (C++, Python, JavaScript)

## License

MIT, see [LICENSE](../LICENSE)

## Contributing

See [docs/contributing/README.md](../docs/contributing/README.md) for contribution guidelines.
