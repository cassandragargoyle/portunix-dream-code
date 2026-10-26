# ADR-004: Code Analysis Plugin Architecture (ptx-codeanalysis)

**Date**: 2026-01-19
**Status**: Proposed
**Author**: Software Architect

## Context

Need to integrate static code analysis tools (SonarLint, JavaCodeScan from Dream project) into Portunix ecosystem. Goals:

- Unified code analysis interface across projects
- YAML configuration in ptxbook style (.ptxcode)
- CI/CD integration with exit codes and thresholds
- Multiple output formats (console, JSON, SARIF, HTML)

### Considered Options

1. **Go helper binary + Java subprocess** - Go binary orchestrates Java engines via subprocess + JSON
2. **Go helper binary + gRPC bridge** - Go binary communicates with Java engines via gRPC
3. **Pure Java plugin** - Entire plugin in Java like text-extractor
4. **Hybrid (subprocess first, gRPC later)** - Start with subprocess, migrate to gRPC

## Decision

Selected **Option 4: Hybrid approach** - start with simpler subprocess/JSON communication, with option to migrate to gRPC for performance.

### Rationale

**Why hybrid:**

1. **Faster MVP** - subprocess doesn't require protobuf schemas and gRPC infrastructure
2. **Easier debugging** - JSON output is human-readable
3. **Flexibility** - gRPC can be added later without changing external API
4. **Existing pattern** - Dream project has similar architecture

**Why Go + Java (not pure Java):**

- Consistency with other Portunix plugins (ptx-ansible is in Go)
- Go for CLI orchestration, Java for heavy lifting (SonarLint, JavaCodeScan)
- Separation of CLI logic from analysis

### Trade-offs

| Aspect | Subprocess/JSON | gRPC | Pure Java |
| ------ | --------------- | ---- | --------- |
| Latency | Higher (spawn) | Low | Lowest |
| Implementation complexity | Low | Medium | Low |
| Debugging | Easy | Medium | Easy |
| Protobuf dependency | No | Yes | No |
| Parallelization | Harder | Easy | Easy |
| **Phase 1** | **Selected** | | |
| **Phase 4+** | | **Migration** | |

## Consequences

### Positive

- **Quick start**: MVP without gRPC infrastructure
- **Testability**: Java bridge can be tested independently
- **Extensibility**: New engines (ESLint) are easy to add
- **Consistency**: .ptxcode format follows .ptxbook pattern

### Negative

- **Higher latency**: Each analysis spawns Java subprocess
- **Memory overhead**: Multiple JVM instances during parallelization
- **Refactoring**: Migration to gRPC requires bridge layer changes

### Mitigation

- **JVM reuse**: Daemon mode for Java bridge (persistent process)
- **Batching**: Group files into single invocation
- **Lazy migration**: gRPC only for hot path (large projects)

## Implementation

### Component Structure

```text
┌─────────────────────────────────────────────────┐
│              portunix analyze                   │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│            ptx-codeanalysis (Go)                │
│  - .ptxcode parser                              │
│  - Engine orchestrator                          │
│  - Report generator                             │
│  - CLI commands                                 │
└──────────────────────┬──────────────────────────┘
                       │ subprocess + JSON
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌─────────────────┐       ┌─────────────────┐
│ ptx-sonarlint   │       │ ptx-javacodescan     │
│ bridge (Java)   │       │ bridge (Java)   │
└─────────────────┘       └─────────────────┘
         │                           │
         ▼                           ▼
   Dream SonarLint            Dream JavaCodeScan
```

### Configuration Example (.ptxcode)

```yaml
apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "my-project"

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
      annotations: [TODO, FIXME]

  thresholds:
    critical: 0
    major: 10

  reports:
    formats:
      - type: console
      - type: sarif
        output: ./analysis.sarif
```

### Exit Codes

| Code | Meaning |
| ---- | ------- |
| 0 | Success - under thresholds |
| 1 | Threshold exceeded |
| 2 | Configuration error |
| 3 | Engine error |
| 4 | Invalid path |

## Related

- Specification: `/portunix-architecture/docs/architecture/specifications/ptx-codeanalysis-plugin.md`
- Issue #013: Code Analysis Plugin Implementation
- Dream project: cd-devtools/sonarlint, cd-devtools/javacodescan
