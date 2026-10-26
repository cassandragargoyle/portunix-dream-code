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

This project is currently in early development. The architecture and core components are being established.

**Last Update**: 2025-10-28 - Project initialized and restructured from previous Dream programming language concept

## Repository Information

- **GitHub Repository**: https://github.com/CassandraGargoyle/portunix-dream-code
- **License**: Proprietary - CassandraGargoyle team only

## Getting Started

Documentation and setup instructions will be added as the project develops.

For development guidelines and contributing information, see:

- [Contributing Guidelines](docs/contributing/README.md)
- [Project Instructions](AGENTS.md)

## Components

| Module | Language | Description |
| ------ | -------- | ----------- |
| codeanalysis | Go + Java | Portunix plugin for code analysis (SonarLint, JavaCodeScan) |
| codeinfo | Java | Code information extraction |

### codeanalysis

**Portunix plugin** - Hybrid Go + Java module for unified code analysis:

- **SonarLint Bridge**: Static analysis for Java, TypeScript, JavaScript, Python
- **JavaCodeScan Bridge**: Annotation detection (TODO, FIXME) and pattern matching
- **YAML Configuration**: `.ptxcode` format for project configuration

See [ADR-004](docs/adr/004-ptx-codeanalysis-plugin-architecture.md) for architecture details.

### codeinfo

**Standalone CLI tool** - Java application for code information extraction:

- **Native Binary**: Compiled with GraalVM Native Image for fast startup
- **Lines of Code**: `ptx-codeinfo loc` command for counting lines of code in Java files
- **JavaParser**: AST-based analysis for accurate code/comment/empty line separation

**Usage**:

```bash
ptx-codeinfo loc MyClass.java           # Basic LOC count
ptx-codeinfo loc MyClass.java -v        # Verbose output with breakdown
```

## Contact & Support

For team members: Please refer to project documentation and internal communication channels.
