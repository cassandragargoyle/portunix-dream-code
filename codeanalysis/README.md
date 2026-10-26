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
portunix analyze init

# Use specific template
portunix analyze init --template java
portunix analyze init --template typescript
portunix analyze init --template python
```

### Run analysis

```bash
# Analyze current directory
portunix analyze

# Analyze specific path
portunix analyze ./src

# Use custom config
portunix analyze --config ./custom.ptxcode

# CI mode with strict exit codes
portunix analyze --ci
```

### Validate configuration

```bash
portunix analyze validate
portunix analyze validate ./project.ptxcode
```

### List available rules

```bash
portunix analyze rules
portunix analyze rules --engine sonarlint --language java
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
portunix analyze → ptx-codeanalysis (Go) → Java bridges → SonarLint/CodeScan
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
