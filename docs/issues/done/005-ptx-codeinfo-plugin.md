# Issue #005: Convert codeinfo to Portunix Plugin (ptx-codeinfo)

**Status**: ✅ Implemented
**Closed**: 2026-10-26
**Acceptance**: [acceptance-005.md](../../testing/acceptance-005.md)
**Priority**: Medium
**GitHub**: #5
**Type**: Refactoring
**Created**: 2026-01-20
**Assigned**: Zdenek
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

- [x] **Task 1.1**: Create `plugin.json` manifest with experimental flag
- [x] **Task 1.2**: Refactor package structure to `org.cassandragargoyle.portunix.codeinfo`
- [x] **Task 1.3**: Create Makefile with build targets (jar, native, test, clean)
- [x] **Task 1.4**: Add `defaults/` directory with sample configurations (README only, no configuration yet)
- [x] **Task 1.5**: Add `testdata/` directory with test fixtures

### Phase 2: Build System

- [x] **Task 2.1**: Update `pom.xml` for plugin artifact naming
- [x] **Task 2.2**: Configure Maven profiles: `jar` (default) and `native` (JAR is the default build without a profile)
- [x] **Task 2.3**: Verify GraalVM native-image build works
- [x] **Task 2.4**: Add fallback JAR execution mode for non-GraalVM environments
- [x] **Task 2.5**: Document GraalVM installation requirements

### Phase 3: Integration with Portunix

- [ ] **Task 3.1**: Register plugin with Portunix plugin registry (out of scope: Portunix repository)
- [ ] **Task 3.2**: Implement plugin discovery mechanism (out of scope: Portunix repository)
- [ ] **Task 3.3**: Add `portunix codeinfo` command integration (out of scope: Portunix repository; `plugin.json` and `make deploy-local` are ready)
- [ ] **Task 3.4**: JSON output format for machine consumption (moved to a separate issue)

### Phase 4: Testing & Documentation

- [x] **Task 4.1**: Unit tests for all analyzers
- [x] **Task 4.2**: Integration tests with sample projects (`testdata/Sample.java`, `PluginManifestTest`)
- [ ] **Task 4.3**: Performance benchmarks (startup time, analysis speed) (moved to a separate issue; current numbers in codeinfo/README.md)
- [x] **Task 4.4**: User documentation
- [x] **Task 4.5**: GraalVM troubleshooting guide

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
