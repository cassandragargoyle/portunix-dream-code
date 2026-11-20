# Portunix Dream Code

**Portunix Dream Code** is a comprehensive toolkit project designed to simplify programming and transform it into a dream-like, effortless experience. This suite of tools aims to make software development more accessible, intuitive, and enjoyable.

## Relationship with Portunix

While **Portunix** provides and simplifies the programming environment for developers (managing dependencies, system setup, cross-platform compatibility), **Portunix Dream Code** focuses on simplifying the programming itself - the act of writing code, designing software, and implementing solutions. Together, they create a complete ecosystem where both the environment and the development process are streamlined.

## Project Vision

The goal of Portunix Dream Code is to create a set of integrated tools that:

- Simplify complex programming tasks
- Reduce boilerplate and repetitive code
- Enable developers to focus on creative problem-solving
- Make programming more accessible to beginners while remaining powerful for experts
- Transform development workflows into streamlined, intuitive processes

## Project Status

The project is in early development. Both modules are Portunix plugins at version 1.1.1;
`code-info` is experimental. See the [changelog of code-info](codeinfo/CHANGELOG.md) and
the [open issues](https://github.com/cassandragargoyle/portunix-dream-code/issues).

## Components

Each module is a Portunix helper plugin. Portunix runs `portunix <plugin> <command>` with
the plugin binary. See [docs/MODULES.md](docs/MODULES.md) for module details.

| Module | Plugin | Binary | Language | Purpose |
| ------ | ------ | ------ | -------- | ------- |
| [codeanalysis](codeanalysis/README.md) | `code-analysis` | `ptx-codeanalysis` | Go + Java bridges | Static code analysis with SonarLint and CodeScan |
| [codeinfo](codeinfo/README.md) | `code-info` | `ptx-codeinfo` | Java (GraalVM Native Image) | Code information and metrics |

### code-analysis

A Go CLI that reads the `.ptxcode` YAML configuration and runs the Java bridges:

- **SonarLint bridge**: static analysis for Java, TypeScript, JavaScript and Python
- **CodeScan bridge**: annotation detection (`TODO`, `FIXME`, `HACK`) and pattern matching
- **CI mode**: strict exit codes based on thresholds (`--ci`)

```bash
portunix code-analysis init --template java       # Create a .ptxcode configuration
portunix code-analysis analyze ./src              # Run the analysis
portunix code-analysis validate ./project.ptxcode # Check a configuration file
portunix code-analysis rules                      # List the available rules
```

Details: [codeanalysis/README.md](codeanalysis/README.md),
[ADR-004](docs/adr/004-ptx-codeanalysis-plugin-architecture.md).

### code-info

A Java CLI (Picocli, JavaParser) built as a JAR and as a GraalVM native image:

- **Lines of code**: the `loc` command counts code, comment and empty lines of a Java file
- **AST-based**: JavaParser separates code and comments correctly, also in mixed lines
- **Fast startup**: the native binary starts in about 2 ms without a JVM

```bash
portunix code-info loc MyClass.java            # Lines of code
portunix code-info loc MyClass.java --verbose  # Breakdown of code, comment and empty lines
```

The binary also runs without Portunix (`ptx-codeinfo loc MyClass.java`).
Details: [codeinfo/README.md](codeinfo/README.md).

## Getting Started

Requirements: Java 21+ and Maven, Go, Python 3.10+ with [uv](https://docs.astral.sh/uv/),
and [Portunix](https://github.com/cassandragargoyle/portunix) to run the plugins.

```bash
make deps          # Install dependencies
make build         # Build both modules
make test          # Run all tests
make deploy-local  # Install both plugins into the local Portunix
```

Run `make help` for all targets. [TESTING.md](TESTING.md) describes the tests and
[DISTRIBUTING.md](DISTRIBUTING.md) the release packages.

## Contributing

- [Contributing Guidelines](docs/contributing/README.md)
- [Project Instructions](AGENTS.md) (also for AI assistants)
- Issues and feature requests: [GitHub Issues](https://github.com/cassandragargoyle/portunix-dream-code/issues)

## Repository Information

- **GitHub Repository**: <https://github.com/cassandragargoyle/portunix-dream-code>
- **License**: MIT, see [LICENSE](LICENSE)
