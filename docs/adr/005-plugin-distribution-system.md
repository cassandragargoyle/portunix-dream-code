# ADR-005: Plugin Distribution System

**Status:** Proposed
**Date:** 2026-01-20
**Author:** Software Architect

## Context

The portunix-dream-code project contains multiple plugins (codeanalysis, codeinfo) that need to be packaged and distributed to end users. Currently, there is no standardized way to create distribution packages.

The sister project `portunix-plugins` has an established distribution system with:

- Python-based `make-release.py` script
- Standardized output structure in `dist/` directory
- Archive generation (tar.gz, zip)
- SHA256 checksums
- Auto-generated release notes

We need a similar system adapted to this project's structure.

### Key Differences from portunix-plugins

| Aspect | portunix-plugins | portunix-dream-code |
| ------ | ---------------- | ------------------- |
| Plugin location | `plugins/{name}/` | Root level (`codeanalysis/`, `codeinfo/`) |
| Plugin types | Java, Go, Python (separate) | Go + Java bridges (hybrid) |
| Build system | Maven or Go per plugin | Mixed (Maven + Go) |
| Artifacts | Single binary/JAR | Binary + bridge JARs |

### codeanalysis Plugin Artifacts

The codeanalysis plugin requires multiple artifacts:

```text
ptx-codeanalysis                              # Go binary (6.8 MB)
bridges/sonarlint/target/ptx-sonarlint-bridge.jar      # Java bridge (3.5 MB)
bridges/javacodescan/target/ptx-javacodescan-bridge.jar # Java bridge (3.5 MB)
plugin.json                                   # Manifest
README.md                                     # Documentation
```

## Decision

Implement a distribution system inspired by portunix-plugins with adaptations for this project's hybrid plugin structure.

### Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                    Distribution Pipeline                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   make dist-codeanalysis VERSION=1.0.0                          │
│        │                                                        │
│        ▼                                                        │
│   ┌─────────────────────────────────────────┐                   │
│   │  Root Makefile                          │                   │
│   │  - Validates VERSION parameter          │                   │
│   │  - Invokes codeanalysis/Makefile dist   │                   │
│   └──────────────────┬──────────────────────┘                   │
│                      │                                          │
│                      ▼                                          │
│   ┌─────────────────────────────────────────┐                   │
│   │  codeanalysis/Makefile dist             │                   │
│   │  - Builds all artifacts (Go + Java)     │                   │
│   │  - Calls make-release.py                │                   │
│   └──────────────────┬──────────────────────┘                   │
│                      │                                          │
│                      ▼                                          │
│   ┌─────────────────────────────────────────┐                   │
│   │  scripts/make-release.py                │                   │
│   │  - Validates artifacts exist            │                   │
│   │  - Creates dist/{plugin}/ directory     │                   │
│   │  - Copies artifacts with versioning     │                   │
│   │  - Creates archives (.tar.gz, .zip)     │                   │
│   │  - Generates SHA256 checksums           │                   │
│   │  - Creates release notes                │                   │
│   └──────────────────┬──────────────────────┘                   │
│                      │                                          │
│                      ▼                                          │
│   ┌─────────────────────────────────────────┐                   │
│   │  dist/ptx-codeanalysis/                 │                   │
│   │  ├── ptx-codeanalysis-1.0.0-linux-amd64 │                   │
│   │  ├── bridges/                           │                   │
│   │  │   ├── ptx-sonarlint-bridge.jar       │                   │
│   │  │   └── ptx-javacodescan-bridge.jar    │                   │
│   │  ├── plugin.json                        │                   │
│   │  └── README.md                          │                   │
│   ├── ptx-codeanalysis-1.0.0-linux-amd64.tar.gz                 │
│   ├── ptx-codeanalysis-1.0.0-linux-amd64.zip                    │
│   ├── checksums-1.0.0-linux-amd64.txt                           │
│   └── RELEASE_NOTES_1.0.0.md                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Distribution Package Structure

```text
dist/
├── ptx-codeanalysis/                    # Working directory
│   ├── ptx-codeanalysis-{version}-{os}-{arch}
│   ├── bridges/
│   │   ├── ptx-sonarlint-bridge.jar
│   │   └── ptx-javacodescan-bridge.jar
│   ├── plugin.json
│   └── README.md
├── ptx-codeanalysis-{version}-{os}-{arch}.tar.gz
├── ptx-codeanalysis-{version}-{os}-{arch}.zip
├── checksums-{version}-{os}-{arch}.txt
└── RELEASE_NOTES_{version}.md
```

### Naming Convention

- **Binary:** `ptx-codeanalysis-{version}-{os}-{arch}[.exe]`
- **Archive:** `ptx-codeanalysis-{version}-{os}-{arch}.{tar.gz|zip}`
- **OS values:** `linux`, `windows`, `darwin`
- **Arch values:** `amd64`, `arm64`

### Makefile Targets

**Root Makefile:**

```makefile
dist: dist-codeanalysis             # Build all distributions
dist-codeanalysis:                   # Build codeanalysis distribution
release: test dist                   # Build with tests
```

**codeanalysis/Makefile:**

```makefile
dist:                                # Build and package for current OS/arch
dist-linux-amd64:                    # Cross-compile for Linux AMD64
dist-windows-amd64:                  # Cross-compile for Windows AMD64
dist-darwin-amd64:                   # Cross-compile for macOS AMD64
dist-all:                            # Build all platform variants
```

### Version Detection

Priority order:

1. Command-line parameter: `make dist VERSION=1.0.0`
2. Auto-detect from `plugin.json` version field
3. Error if neither available

### Cross-Platform Builds

Go supports cross-compilation via `GOOS` and `GOARCH`:

```bash
GOOS=windows GOARCH=amd64 go build -o ptx-codeanalysis.exe
GOOS=darwin GOARCH=amd64 go build -o ptx-codeanalysis
GOOS=linux GOARCH=arm64 go build -o ptx-codeanalysis
```

Java bridge JARs are platform-independent (same JAR for all platforms).

## Consequences

### Positive

1. **Consistency** - Same distribution approach as portunix-plugins
2. **Automation** - Single command creates complete release package
3. **Verification** - SHA256 checksums for integrity checking
4. **Cross-platform** - Support for Linux, Windows, macOS (amd64, arm64)
5. **CI/CD ready** - Easy integration with GitHub Actions

### Negative

1. **Complexity** - Hybrid plugin (Go + Java) requires more packaging logic
2. **Size** - Full package is ~14 MB (Go binary + 2 Java JARs)
3. **Dependencies** - Requires Python 3 for make-release.py script

### Neutral

1. Java bridges remain platform-independent (no per-OS variants needed)
2. Version must be kept in sync between plugin.json and releases

## Implementation Checklist

- [ ] Create `scripts/make-release.py` adapted for this project
- [ ] Add `dist` target to `codeanalysis/Makefile`
- [ ] Add `dist-codeanalysis` target to root `Makefile`
- [ ] Add `.gitignore` entry for `dist/` directory
- [ ] Test distribution on Linux
- [ ] Document usage in README
