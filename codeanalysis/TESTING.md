# Testing Guide

Integration tests for ptx-codeanalysis use the **pytest** framework.

## Requirements

Tests use a Python environment managed by [uv](https://docs.astral.sh/uv/).
Dependencies are defined in `../pyproject.toml` and locked in `../uv.lock`.

```bash
# Sync environment manually (optional, make test does this automatically)
make setup-venv   # or: uv sync
```

The environment is located at `../.venv/` and includes pytest.

## Running Tests

### From project root

```bash
make test-codeanalysis    # All codeanalysis tests
make test-quick           # Quick tests only
make test-codescan        # CodeScan tests only
```

### From codeanalysis directory

```bash
cd codeanalysis

make test                 # All tests
make test-quick           # Quick tests only
make test-codescan        # CodeScan tests only
make test-integration     # Integration tests
```

### Using pytest directly (via uv)

```bash
cd codeanalysis

# All tests
uv run pytest test/ -v

# Quick tests
uv run pytest test/ -v -m quick

# CodeScan tests
uv run pytest test/ -v -m codescan

# Specific test
uv run pytest test/integration/test_codescan.py::TestCodeScanBridge::test_detect_todo_in_java -v

# Filter by name
uv run pytest test/ -v -k "todo"
```

## Test Structure

```
codeanalysis/
├── test/
│   ├── conftest.py              # Pytest configuration and fixtures
│   ├── run-tests.py             # Helper script
│   └── integration/
│       └── test_codescan.py     # CodeScan integration tests
├── testdata/
│   ├── Sample.java
│   ├── sample.ts
│   └── sample.py
└── pytest.ini
```

## Markers

| Marker | Description |
|--------|-------------|
| `quick` | Quick tests for development |
| `codescan` | CodeScan bridge tests |
| `sonarlint` | SonarLint bridge tests |
| `integration` | Integration tests requiring bridges |

Example:

```bash
uv run pytest test/ -m quick
uv run pytest test/ -m "codescan and quick"
uv run pytest test/ -m "not slow"
```

## Fixtures

Available fixtures in `conftest.py`:

| Fixture | Description |
|---------|-------------|
| `project_root` | Path to codeanalysis directory |
| `testdata_dir` | Path to testdata directory |
| `codescan_bridge` | Path to CodeScan JAR (skips if not found) |
| `sonarlint_bridge` | Path to SonarLint JAR (skips if not found) |
| `java_available` | Checks Java availability |

## Before Running Tests

Build the bridges first:

```bash
make build-bridge
```

If bridges are not built, tests will be skipped automatically.

## Adding New Tests

1. Create file `test/integration/test_*.py`
2. Use fixtures from `conftest.py`
3. Mark tests with appropriate markers

```python
import pytest

class TestMyFeature:

    @pytest.mark.quick
    @pytest.mark.codescan
    def test_something(self, codescan_bridge, testdata_dir):
        """Test description."""
        # ... test code ...
        assert result == expected
```

## Debugging

```bash
uv run pytest test/ -v      # Verbose output
uv run pytest test/ -v -s   # Show print statements
uv run pytest test/ -v -x   # Stop on first failure
uv run pytest test/ --lf    # Run last failed tests
uv run pytest test/ --pdb   # Debug on failure
```

## CI/CD Integration

```bash
# Exit code 0 = all tests passed
make test-codeanalysis

# With JUnit XML report
uv run pytest test/ --junitxml=test-results.xml
```
