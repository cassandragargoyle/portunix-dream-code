# Issue #005: Convert codeinfo to Portunix Plugin (ptx-codeinfo)

**Status**: Open
**Priority**: Medium
**GitHub**: #5
**Type**: Refactoring
**Created**: 2026-01-20
**Assigned**: TBD
**Estimated Effort**: 40-60 hours
**Target Version**: 1.3.0

## Description

Restructure the experimental `codeinfo` module into a proper Portunix plugin, following the same plugin architecture pattern as `ptx-codeanalysis`. The module already produces a native binary `ptx-codeinfo` via GraalVM Native Image with fast startup (<50ms). This issue focuses on:

- Adding plugin metadata (`plugin.json`)
- Refactoring package structure for Portunix namespace
- Integrating with Portunix plugin discovery
- Standardizing build and directory structure

## ⚠️ Experimental Status

This module is **experimental** with the following considerations:

- **GraalVM Native Image**: Requires GraalVM 23.1.0+ for native compilation
- **JavaParser reflection**: Requires tracing agent-generated reflection configs
- **Binary size**: Native image may be 15-25MB
- **Build complexity**: Native compilation adds significant build time

## Current State

### Directory Structure (Before)

```
codeinfo/
├── src/main/java/org/cassandragargoyle/dream/codeinfo/
│   ├── PtxCodeInfoCLI.java              # Picocli @Command entry point
│   ├── commands/
│   │   └── LinesOfCodeCommand.java
│   └── analyzers/
│       ├── JavaLocAnalyzer.java
│       └── LocResult.java
├── src/main/resources/META-INF/native-image/
│   ├── native-image.properties
│   └── reflect-config.json
├── pom.xml
└── README.md
```

### Key Technologies

| Component | Version | GraalVM Support |
|-----------|---------|-----------------|
| Java | 21+ | Required |
| Picocli | 4.7.5 | ✅ Excellent |
| JavaParser | 3.27.1 | ⚠️ Requires reflection config |
| GraalVM SDK | 23.1.0 | Native |

## Target State

### Directory Structure (After)

```
codeinfo/
├── cmd/                                # Optional: Go wrapper (if needed)
├── src/main/java/org/cassandragargoyle/portunix/codeinfo/
│   ├── Main.java                       # Entry point
│   ├── commands/
│   │   ├── LinesOfCodeCommand.java
│   │   └── ...                         # Future commands
│   └── analyzers/
│       ├── JavaLocAnalyzer.java
│       └── LocResult.java
├── src/main/resources/META-INF/native-image/
│   ├── native-image.properties
│   └── reflect-config.json
├── defaults/                           # Default config templates
├── testdata/                           # Test fixtures
├── plugin.json                         # Plugin manifest
├── pom.xml
├── Makefile
└── README.md
```

### Plugin Manifest (plugin.json)

```json
{
  "name": "ptx-codeinfo",
  "version": "1.0.0",
  "description": "Fast code information and metrics analysis",
  "type": "helper",
  "experimental": true,
  "binary": "ptx-codeinfo",
  "runtime": "native",
  "runtimeDetails": {
    "technology": "graalvm-native-image",
    "graalvmVersion": "23.1.0+"
  },
  "commands": [
    {
      "name": "loc",
      "description": "Count lines of code"
    }
  ],
  "requirements": {
    "portunix": ">=1.10.0",
    "java": ">=21"
  }
}
```

## Tasks

### Phase 1: Plugin Structure

- [ ] **Task 1.1**: Create `plugin.json` manifest with experimental flag
- [ ] **Task 1.2**: Refactor package structure to `org.cassandragargoyle.portunix.codeinfo`
- [ ] **Task 1.3**: Create Makefile with build targets (jar, native, test, clean)
- [ ] **Task 1.4**: Add `defaults/` directory with sample configurations
- [ ] **Task 1.5**: Add `testdata/` directory with test fixtures

### Phase 2: Build System

- [ ] **Task 2.1**: Update `pom.xml` for plugin artifact naming
- [ ] **Task 2.2**: Configure Maven profiles: `jar` (default) and `native`
- [ ] **Task 2.3**: Verify GraalVM native-image build works
- [ ] **Task 2.4**: Add fallback JAR execution mode for non-GraalVM environments
- [ ] **Task 2.5**: Document GraalVM installation requirements

### Phase 3: Integration with Portunix

- [ ] **Task 3.1**: Register plugin with Portunix plugin registry
- [ ] **Task 3.2**: Implement plugin discovery mechanism
- [ ] **Task 3.3**: Add `portunix codeinfo` command integration
- [ ] **Task 3.4**: JSON output format for machine consumption

### Phase 4: Testing & Documentation

- [ ] **Task 4.1**: Unit tests for all analyzers
- [ ] **Task 4.2**: Integration tests with sample projects
- [ ] **Task 4.3**: Performance benchmarks (startup time, analysis speed)
- [ ] **Task 4.4**: User documentation
- [ ] **Task 4.5**: GraalVM troubleshooting guide

## Architecture Decision

### Option A: Pure Java/GraalVM Native (Recommended)

Keep the current Java-only architecture with GraalVM native compilation.

**Pros:**
- Simpler architecture (single language)
- Already working with GraalVM
- Fast startup (<50ms) achieved
- Easier maintenance

**Cons:**
- Different pattern than codeanalysis (Go + Java)
- Requires GraalVM for native builds

### Option B: Go Wrapper + Java Backend

Add a Go CLI wrapper similar to codeanalysis.

**Pros:**
- Consistent with codeanalysis architecture
- Go provides reliable CLI experience

**Cons:**
- Duplicates GraalVM native benefits
- More complex build (Go + Java + GraalVM)
- Over-engineering for current scope

**Decision**: Use **Option A** (Pure Java/GraalVM) due to experimental status and existing GraalVM integration. Can migrate to Option B later if needed.

## Dependencies

- Java 21+
- GraalVM 23.1.0+ (for native builds)
- Maven 3.9+
- Picocli 4.7.5
- JavaParser 3.27.1

## Risks

| Risk | Mitigation |
|------|------------|
| GraalVM version incompatibility | Document supported versions, test on CI |
| JavaParser reflection issues | Maintain reflect-config.json, use tracing agent |
| Large binary size (>20MB) | Acceptable for experimental, optimize later |
| Build time for native image | Provide JAR fallback for development |

## Related

- Issue #004: Code Analysis Plugin (ptx-codeanalysis)
- Issue #002: Implement ptx-codeinfo CLI Application
- GRAALVM-COMPATIBILITY.md in codeinfo module

## Notes

- This is an **experimental** module - expect breaking changes
- GraalVM Native Image compilation is optional; JAR execution always works
- Performance targets: <50ms startup, <100ms for single file analysis
