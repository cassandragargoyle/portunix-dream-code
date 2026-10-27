# Testing Guide

This document describes how to run tests for portunix-dream-code project.

## Quick Start

```bash
# Run all tests
make test

# Run quick tests only
make test-quick
```

## Test Commands

| Command | Description |
| ------- | ----------- |
| `make test` | Run all tests (Java + codeanalysis) |
| `make test-java` | Run Java module tests only |
| `make test-codeinfo` | Run codeinfo tests only |
| `make coverage-codeinfo` | Run codeinfo tests with 95% line coverage gate |
| `make test-codeinfo-native` | Compare codeinfo native binary with JAR (after `make build-codeinfo-native`) |
| `make test-codeanalysis` | Run codeanalysis tests only |
| `make test-go` | Run Go vet, build and unit tests (codeanalysis) |
| `make test-quick` | Run quick tests (codeanalysis) |
| `make test-codescan` | Run CodeScan bridge tests |

## Requirements

### Java Tests

- Java 21+
- Maven 3.6+

### Python Tests (codeanalysis)

- Python 3.10+
- [uv](https://docs.astral.sh/uv/)

Python dependencies are defined in `pyproject.toml` and locked in `uv.lock`.
The environment is created by uv automatically:

```bash
# Sync environment manually (optional, make test does this automatically)
make setup-venv   # or: uv sync
```

The environment is located at `.venv/` and includes pytest.

## Project Structure

```text
portunix-dream-code/
├── codeinfo/                    # Java module (Portunix plugin ptx-codeinfo)
│   ├── src/test/java/           # Java unit tests
│   ├── src/test/resources/      # Unit test fixtures
│   └── testdata/                # Samples for manual and integration tests
├── codeanalysis/                # Go + Java bridges
│   ├── test/                    # Python integration tests
│   ├── testdata/                # Test fixtures
│   └── TESTING.md               # Detailed testing guide
└── TESTING.md                   # This file
```

## Running Tests by Module

### codeinfo (Java)

```bash
make test-codeinfo
# or
make -C codeinfo test
```

### codeanalysis (Python + Go + Java)

```bash
make test-codeanalysis
# or
cd codeanalysis && make test
```

See [codeanalysis/TESTING.md](codeanalysis/TESTING.md) for detailed instructions.

## Before Running Tests

Build the project first:

```bash
make build
```

For codeanalysis, ensure bridges are built:

```bash
make build-codeanalysis
```

## Continuous Integration

GitHub Actions workflow [.github/workflows/pr-checks.yml](.github/workflows/pr-checks.yml)
runs on every pull request to `main` and on every push to `main` (`ubuntu-latest`).
A failing build or test makes the check red.

| Job | Command | Checks |
| --- | ------- | ------ |
| Java (codeinfo) | `make build-java`, `make test-java` | Maven build and JUnit tests |
| Go (codeanalysis) | `make test-go` | `go vet`, `go build`, `go test` |
| Java bridge (SonarLint) | `make -C codeanalysis/bridges build-sonarlint test-sonarlint` | Bridge build and JUnit tests |

Maven and Go module caches are enabled.

### Manual checks (not in CI)

Run these locally before a release or when you change the related code:

- CodeScan bridge and Python tests (`make test-codeanalysis`, `make test-quick`,
  `make test-codescan`) – the bridge depends on `dream-code-devtools`, which is not
  in a public Maven repository
- GraalVM native image (`make build-codeinfo-native`, `make test-codeinfo-native`)
- Coverage gate (`make coverage-codeinfo`)
- Windows and macOS

### Local equivalent

```bash
# Full test suite with exit code
make test

# Release with tests
make release
```
