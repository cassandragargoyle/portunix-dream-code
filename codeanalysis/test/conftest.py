# SPDX-License-Identifier: MIT
"""
pytest configuration and shared fixtures for codeanalysis tests.
"""

import os
import pytest
import subprocess
import json
from pathlib import Path

# Set in CI: a missing bridge or Java fails the test instead of skipping it
REQUIRE_BRIDGES = os.environ.get("PTX_REQUIRE_BRIDGES") == "1"


def _missing(reason: str):
    """Skip the test, or fail it when PTX_REQUIRE_BRIDGES=1."""
    if REQUIRE_BRIDGES:
        pytest.fail(f"{reason} (PTX_REQUIRE_BRIDGES=1)")
    pytest.skip(reason)


@pytest.fixture(scope="session")
def project_root():
    """Project root directory fixture."""
    return Path(__file__).parent.parent


@pytest.fixture(scope="session")
def testdata_dir(project_root):
    """Testdata directory fixture."""
    return project_root / "testdata"


@pytest.fixture(scope="session")
def codescan_bridge(project_root):
    """CodeScan bridge JAR path fixture."""
    jar_path = project_root / "bridges" / "codescan" / "target" / "ptx-codescan-bridge.jar"
    if not jar_path.exists():
        _missing(f"CodeScan bridge not found at {jar_path}. Run 'make build-bridge' first.")
    return jar_path


@pytest.fixture(scope="session")
def sonarlint_bridge(project_root):
    """SonarLint bridge JAR path fixture."""
    jar_path = project_root / "bridges" / "sonarlint" / "target" / "ptx-sonarlint-bridge.jar"
    if not jar_path.exists():
        _missing(f"SonarLint bridge not found at {jar_path}. Run 'make build-bridge' first.")
    return jar_path


@pytest.fixture(scope="session")
def java_available():
    """Check if Java is available."""
    try:
        result = subprocess.run(["java", "-version"], capture_output=True)
        if result.returncode != 0:
            _missing("Java is not available")
        return True
    except FileNotFoundError:
        _missing("Java is not installed")


def run_bridge(jar_path: Path, command: str, input_data: dict) -> dict:
    """Helper to run a bridge command and return JSON response."""
    input_json = json.dumps(input_data)

    result = subprocess.run(
        ["java", "-jar", str(jar_path), command],
        input=input_json,
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        raise RuntimeError(f"Bridge failed: {result.stderr}")

    return json.loads(result.stdout)


def pytest_configure(config):
    """Configure pytest with custom markers."""
    config.addinivalue_line("markers", "quick: Quick tests for development")
    config.addinivalue_line("markers", "slow: Slow tests that take time")
    config.addinivalue_line("markers", "integration: Integration tests requiring bridges")
    config.addinivalue_line("markers", "codescan: Tests for CodeScan bridge")
    config.addinivalue_line("markers", "sonarlint: Tests for SonarLint bridge")


def pytest_collection_modifyitems(config, items):
    """Add markers to tests based on their location."""
    for item in items:
        if "integration" in str(item.fspath):
            item.add_marker(pytest.mark.integration)


def pytest_report_header(config):
    """Add custom header to pytest output."""
    return [
        "ptx-codeanalysis Test Suite",
        "Bridges: CodeScan, SonarLint",
        "Test Framework: pytest"
    ]
