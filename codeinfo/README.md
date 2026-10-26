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
# Build JAR
mvn clean package

# Build native image (requires GraalVM)
mvn clean package -Pnative
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

### Counting Rules

The LOC counter follows these rules:

- **Count**: Lines with executable code
- **Count**: Lines with both code and comments (e.g., `int x = 1; // comment`)
- **Skip**: Empty lines (whitespace only)
- **Skip**: Comment-only lines (single-line `//` or multi-line `/* */`)

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

```bash
mvn test
```

### Building

```bash
# Build JAR only
mvn clean package

# Build JAR and run tests
mvn clean verify

# Build native image (requires GraalVM)
mvn clean package -Pnative
```

### Running from JAR

```bash
java -jar target/dream-code-codeinfo-1.0.0.2-SNAPSHOT.jar loc Example.java
```

## Architecture

This tool uses:

- **Picocli 4.7.7** for CLI parsing and command structure (excellent GraalVM support)
- **JavaParser 3.27.1** for Java source code parsing and AST analysis
- **GraalVM Community Edition** for ahead-of-time compilation to native binary

See [ADR-002](../docs/adr/002-ptx-codeinfo-cli-application.md) for architectural decisions.

See [GRAALVM-COMPATIBILITY.md](GRAALVM-COMPATIBILITY.md) for detailed information about GraalVM Native Image compatibility and build instructions.

## Performance Targets

| Metric | Target | Description |
| ------ | ------ | ----------- |
| Startup Time | < 50ms | Time to show help |
| Single File (100 LOC) | < 100ms | Analysis time |
| Large File (5000 LOC) | < 500ms | Analysis time |
| Binary Size | < 20MB | Native binary size |
| Memory Usage | < 30MB | Runtime memory |

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
