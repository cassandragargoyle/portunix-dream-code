# ptx-codeinfo - Fast Code Analysis CLI

> **⚠️ Experimental Project**: This is an experimental project exploring GraalVM Native Image capabilities for building high-performance CLI tools. APIs and features may change without notice.

A high-performance code analysis tool powered by GraalVM Native Image, Picocli, and JavaParser.

## Features

- **Fast Startup**: Sub-50ms execution time with GraalVM Native Image
- **Lines of Code Counter**: Accurate LOC counting for Java source files
- **Zero Dependencies**: Native binary with no JVM required
- **Cross-Platform**: Binaries for Linux and Windows

## Installation

### From Binary (Recommended)

Download the latest binary for your platform from the releases page and add it to your PATH.

### From Source

Requirements:

- GraalVM 23.1.0 or later
- Maven 3.8+
- Java 21

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

### Exit Codes

| Code | Meaning |
| ---- | ------- |
| 0 | Success (also for files with syntax errors, see below) |
| 1 | Unexpected error during analysis |
| 2 | Invalid input: file not found, not a regular file, not readable, not `.java`, not valid UTF-8, or missing argument |

### Counting Rules

The LOC counter follows these rules:

- **Count**: Lines with executable code
- **Count**: Lines with both code and comments (e.g., `int x = 1; // comment`)
- **Skip**: Empty lines (whitespace only)
- **Skip**: Comment-only lines (single-line `//` or multi-line `/* */`)

Comments are detected by the Java lexer, so comment markers inside string literals
and text blocks are not treated as comments. A line with several comments and no code
(e.g. `/* a */ /* b */`) is a comment line.

Files are read as UTF-8. Files with syntax errors are still counted: a warning is
printed to stderr and the counts are based on lexical analysis only.

## Development

### Project Structure

```text
codeinfo/
├── src/main/java/org/cassandragargoyle/dream/codeinfo/
│   ├── PtxCodeInfoCLI.java          # Main entry point
│   ├── commands/
│   │   └── LinesOfCodeCommand.java   # LOC command implementation
│   └── analyzers/
│       ├── JavaLocAnalyzer.java      # LOC analysis logic
│       └── LocResult.java            # Result container
├── src/test/java/                    # Unit tests
└── src/test/resources/fixtures/      # Test fixtures
```

### Running Tests

Run from the repository root:

```bash
# Unit tests (analyzer and loc command)
make test-codeinfo

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

### Running from JAR

```bash
java -jar codeinfo/target/dream-code-codeinfo-1.0.0.2-SNAPSHOT.jar loc Example.java
```

## Architecture

This tool uses:

- **Picocli 4.7.7** for CLI parsing and command structure (excellent GraalVM support)
- **JavaParser 3.27.1** for Java source code parsing and AST analysis
- **GraalVM Community Edition** for ahead-of-time compilation to native binary

See [ADR-002](../docs/adr/002-ptx-codeinfo-cli-application.md) for architectural decisions.

See [GRAALVM-COMPATIBILITY.md](GRAALVM-COMPATIBILITY.md) for detailed information about GraalVM Native Image compatibility and build instructions.

## Performance

Measured on Linux x64 with GraalVM CE 21.0.2 (median of 20 runs, peak RSS).
The JAR runs on Temurin 21 for comparison.

| Metric | Target | Native | JAR |
| ------ | ------ | ------ | --- |
| Startup Time (`--version`) | < 50ms | 1.8 ms | 77 ms |
| Single File (100 LOC) | < 100ms | 2.6 ms | 159 ms |
| Large File (5000 LOC) | < 500ms | 132 ms | 391 ms |
| Binary Size | < 20MB | 22.7 MB | - |
| Memory Usage (100 LOC) | < 30MB | 20.6 MB | 80 MB |
| Memory Usage (5000 LOC) | < 30MB | 110 MB | 317 MB |

Binary size and memory for large files are above target. Memory grows with file size
because the whole syntax tree is kept in memory.

## Future Enhancements

- Cyclomatic complexity analysis
- Import dependency analysis
- Batch processing for directories
- Multiple output formats (JSON, CSV, XML)
- Multi-language support (C++, Python, JavaScript)

## License

Proprietary - CassandraGargoyle team only

## Contributing

See [docs/contributing/README.md](../docs/contributing/README.md) for contribution guidelines.
