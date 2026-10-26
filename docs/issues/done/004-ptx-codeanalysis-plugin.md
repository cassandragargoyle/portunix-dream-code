# Issue #004: Code Analysis Plugin (ptx-codeanalysis)

**Status**: Completed
**Priority**: High
**GitHub**: #4
**Type**: New Feature
**Created**: 2026-01-19
**Assigned**: TBD
**Estimated Effort**: 80-100 hours
**Target Version**: 1.2.0

## Description

Implement a unified code analysis plugin integrating SonarLint and CodeScan engines. The plugin provides YAML-based configuration (.ptxcode), multiple output formats, and CI/CD integration with exit codes and thresholds.

## Related ADR

See [ADR-004: Code Analysis Plugin Architecture](../../adr/004-ptx-codeanalysis-plugin-architecture.md) for architectural decision and rationale.

## Scope

### Components

1. **Go CLI** (`ptx-codeanalysis`) - Orchestrator and CLI interface
2. **SonarLint Bridge** (Java) - Static code analysis for multiple languages
3. **JavaCodeScan Bridge** (Java) - Annotation detection and pattern matching

### Directory Structure

```
codeanalysis/
├── cmd/ptx-codeanalysis/    # Go main entry
├── internal/
│   ├── cli/                 # CLI commands
│   ├── config/              # YAML parser
│   ├── engine/              # Engine orchestration
│   └── report/              # Output formatters
├── bridges/
│   ├── sonarlint/           # Java SonarLint bridge
│   └── javacodescan/        # Java annotation scanner
├── defaults/                # Config templates
└── testdata/                # Test files
```

## Features

### Configuration (.ptxcode)

```yaml
apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "project-analysis"

spec:
  targets:
    include: ["src/**/*.java", "src/**/*.ts"]
    exclude: ["**/build/**"]

  engines:
    sonarlint:
      enabled: true
      languages: [java, typescript]
    javacodescan:
      enabled: true
      annotations: [TODO, FIXME, HACK]

  thresholds:
    blocker: 0
    critical: 5

  reports:
    formats:
      - type: console
      - type: sarif
        output: ./analysis.sarif
```

### Exit Codes

| Code | Meaning |
| ---- | ------- |
| 0 | Success - no issues above threshold |
| 1 | Threshold exceeded |
| 2 | Configuration error |
| 3 | Engine error |
| 4 | Invalid path |

## Tasks

### Phase 1: Foundation ✅ COMPLETED

- [x] **Task 1.1**: Create Go CLI structure with Cobra
- [x] **Task 1.2**: Implement .ptxcode YAML parser
- [x] **Task 1.3**: Create SonarLint Java bridge
- [x] **Task 1.4**: Implement subprocess communication (JSON)
- [x] **Task 1.5**: Console output formatter

### Phase 2: JavaCodeScan Engine ✅ COMPLETED

- [x] **Task 2.1**: Create JavaCodeScan Java bridge
- [x] **Task 2.2**: Implement annotation detection (TODO, FIXME, HACK, etc.)
- [x] **Task 2.3**: Add custom pattern matching support
- [x] **Task 2.4**: Integrate with Go orchestrator
- [x] **Task 2.5**: Update configuration schema

### Phase 3: Output Formats (PENDING)

- [ ] **Task 3.1**: JSON output format
- [ ] **Task 3.2**: SARIF output format
- [ ] **Task 3.3**: HTML report generator

### Phase 4: CI/CD Integration (PENDING)

- [ ] **Task 4.1**: Threshold validation and exit codes
- [ ] **Task 4.2**: CI mode (--ci flag)
- [ ] **Task 4.3**: Baseline support for diff analysis

### Phase 5: Testing & Documentation (PENDING)

- [ ] **Task 5.1**: Go unit tests
- [ ] **Task 5.2**: Java unit tests
- [ ] **Task 5.3**: Integration tests
- [ ] **Task 5.4**: User documentation

## Dependencies

- Go 1.21+
- Java 17+
- SonarLint libraries

## Notes

- Phase 1 and 2 completed in portunix-plugins, moved to portunix-dream-code
- JavaCodeScan renamed from TvkCode for better clarity
