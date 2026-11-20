# ptx-codeanalysis

Code analysis plugin for Portunix integrating SonarLint and third party engines.

## Features

- **Unified Interface**: Single CLI for multiple analysis engines
- **YAML Configuration**: `.ptxcode` configuration format (similar to ptxbook)
- **CI/CD Ready**: Exit codes and thresholds for automated pipelines
- **Multiple Output Formats**: Console, JSON, SARIF, HTML
- **Multiple Engines**:
  - **SonarLint**: Static code analysis for Java, TypeScript, JavaScript, Python
  - **CodeScan**: Annotation detection (TODO, FIXME, HACK) and custom pattern matching

## Requirements

- Go 1.21+ (for building)
- Java 21+ (for bridges)

## Installation

### Build from source

```bash
# Build everything
make build

# Or build components separately
make build-go      # Go CLI binary
make build-bridge  # All Java bridges
```

## Usage

### Initialize configuration

```bash
# Create default .ptxcode file
portunix code-analysis init

# Use specific template
portunix code-analysis init --template java
portunix code-analysis init --template typescript
portunix code-analysis init --template python
```

### Run analysis

```bash
# Analyze current directory
portunix code-analysis analyze

# Analyze specific path
portunix code-analysis analyze ./src

# Use custom config
portunix code-analysis analyze --config ./custom.ptxcode

# CI mode with strict exit codes
portunix code-analysis analyze --ci
```

### Validate configuration

```bash
portunix code-analysis validate
portunix code-analysis validate ./project.ptxcode
```

### List available rules

```bash
portunix code-analysis rules
portunix code-analysis rules --engine sonarlint --language java
```

## Configuration

### Minimal configuration

```yaml
apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "my-project"

spec:
  targets:
    include: ["src/**/*"]
  engines:
    sonarlint:
      enabled: true
```

### Full configuration

```yaml
apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "project-analysis"
  description: "Code quality analysis"

spec:
  targets:
    include:
      - "src/**/*.java"
      - "src/**/*.ts"
    exclude:
      - "**/build/**"
      - "**/node_modules/**"

  engines:
    sonarlint:
      enabled: true
      languages: [java, typescript]
      rules:
        disable: ["java:S106"]

    codescan:
      enabled: true
      annotations: [TODO, FIXME, HACK]

  thresholds:
    blocker: 0
    critical: 5
    major: 50

  reports:
    formats:
      - type: console
      - type: sarif
        output: ./analysis.sarif
```

### Target patterns

`spec.targets.include` and `spec.targets.exclude` are glob patterns:

- Patterns are relative to the directory of the `.ptxcode` file (the project root),
  also when `analyze` gets a subdirectory or a single file as the target. With
  `include: ["src/main/java/**/*.java"]`, `analyze ./src/main` analyzes
  `src/main/java/...` files
- Without a `.ptxcode` file, patterns are relative to the analysis target (or to the
  directory of a target file)
- A file outside the `.ptxcode` directory is matched relative to the analysis target
- `/` is the separator on all platforms, also on Windows
- `**` matches zero or more directories at the start, in the middle or at the end:
  `**/*.java`, `src/**/*.ts`, `**/target/**`
- `*`, `?` and `[...]` match within one path segment, `*` never matches `/`
- A file is analyzed when it matches an include pattern and no exclude pattern
- An exclude pattern with a trailing `/**` (for example `**/node_modules/**`) removes
  the whole directory, it is not walked

## Exit Codes

| Code | Meaning |
| ---- | ------- |
| 0 | Success - no issues above threshold |
| 1 | Analysis failed - issues exceed threshold |
| 2 | Configuration error |
| 3 | Engine error (SonarLint/CodeScan failed) |
| 4 | Invalid target path |

## Supported Languages

- Java
- JavaScript
- TypeScript
- Python
- Go (limited)
- Kotlin (limited)

## Architecture

```text
portunix code-analysis → ptx-codeanalysis (Go) → Java bridges → SonarLint/CodeScan
```

The Go binary orchestrates analysis and communicates with Java bridges via subprocess + JSON.

## Development

```bash
# Run tests
make test

# Quick development build
make dev

# Clean build artifacts
make clean
```

## Related

- [Issue #4](../docs/issues/done/004-ptx-codeanalysis-plugin.md): Implementation details
- [ADR-004](../../docs/adr/004-ptx-codeanalysis-plugin-architecture.md): Architecture decision
