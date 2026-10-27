"""
Integration tests for CodeScan bridge.

Run with:
    pytest test/integration/test_codescan.py -v
    pytest test/integration/test_codescan.py -v -m quick
"""

import pytest
import json
import subprocess
from pathlib import Path


class TestCodeScanBridge:
    """Integration tests for CodeScan bridge."""

    @pytest.fixture
    def run_codescan(self, codescan_bridge, java_available):
        """Fixture to run codescan analysis."""
        def _run(request_data: dict) -> dict:
            input_json = json.dumps(request_data)
            result = subprocess.run(
                ["java", "-jar", str(codescan_bridge), "analyze"],
                input=input_json,
                capture_output=True,
                text=True
            )
            assert result.returncode == 0, f"Bridge failed: {result.stderr}"
            return json.loads(result.stdout)
        return _run

    @pytest.mark.quick
    @pytest.mark.codescan
    def test_detect_todo_in_java(self, run_codescan, testdata_dir):
        """Test detection of TODO comments in Java file."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["TODO"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert len(response["issues"]) >= 1

        todo_issues = [i for i in response["issues"] if "TODO" in i["ruleKey"]]
        assert len(todo_issues) >= 1
        assert todo_issues[0]["ruleKey"] == "codescan:TODO"

    @pytest.mark.quick
    @pytest.mark.codescan
    def test_detect_fixme_in_java(self, run_codescan, testdata_dir):
        """Test detection of FIXME comments in Java file."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["FIXME"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        fixme_issues = [i for i in response["issues"] if "FIXME" in i["ruleKey"]]
        assert len(fixme_issues) >= 1
        assert fixme_issues[0]["ruleKey"] == "codescan:FIXME"

    @pytest.mark.codescan
    def test_detect_multiple_annotations(self, run_codescan, testdata_dir):
        """Test detection of multiple annotation types."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["TODO", "FIXME", "HACK", "XXX"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        rule_keys = {i["ruleKey"] for i in response["issues"]}

        # Sample.java has TODO and FIXME
        assert "codescan:TODO" in rule_keys
        assert "codescan:FIXME" in rule_keys

    @pytest.mark.codescan
    def test_typescript_file(self, run_codescan, testdata_dir):
        """Test analysis of TypeScript file."""
        request = {
            "files": [str(testdata_dir / "sample.ts")],
            "annotations": ["TODO", "FIXME"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert len(response["issues"]) >= 2

        # Verify language detection
        for issue in response["issues"]:
            assert issue["language"] == "typescript"

    @pytest.mark.codescan
    def test_python_file(self, run_codescan, testdata_dir):
        """Test analysis of Python file."""
        request = {
            "files": [str(testdata_dir / "sample.py")],
            "annotations": ["TODO", "FIXME", "HACK", "XXX"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert len(response["issues"]) >= 4  # TODO, FIXME, HACK, XXX

        # Verify language detection
        for issue in response["issues"]:
            assert issue["language"] == "python"

    @pytest.mark.codescan
    def test_multiple_files(self, run_codescan, testdata_dir):
        """Test analysis of multiple files at once."""
        request = {
            "files": [
                str(testdata_dir / "Sample.java"),
                str(testdata_dir / "sample.ts"),
                str(testdata_dir / "sample.py")
            ],
            "annotations": ["TODO"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert response["stats"]["filesAnalyzed"] == 3

        # Should find TODO in all files
        assert len(response["issues"]) >= 3

    @pytest.mark.codescan
    def test_severity_mapping(self, run_codescan, testdata_dir):
        """Test custom severity mapping."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["TODO", "FIXME"],
            "severityMapping": {
                "TODO": "INFO",
                "FIXME": "CRITICAL"
            }
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None

        for issue in response["issues"]:
            if issue["ruleKey"] == "codescan:TODO":
                assert issue["severity"] == "INFO"
            elif issue["ruleKey"] == "codescan:FIXME":
                assert issue["severity"] == "CRITICAL"

    @pytest.mark.codescan
    def test_no_annotations_found(self, run_codescan, testdata_dir):
        """Test when no matching annotations are found."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["NONEXISTENT"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert len(response["issues"]) == 0

    @pytest.mark.codescan
    def test_empty_file_list(self, run_codescan):
        """Test with empty file list."""
        request = {
            "files": [],
            "annotations": ["TODO"]
        }

        response = run_codescan(request)

        assert "error" not in response or response.get("error") is None
        assert len(response["issues"]) == 0
        assert response["stats"]["filesAnalyzed"] == 0

    @pytest.mark.codescan
    def test_rulekey_prefix(self, run_codescan, testdata_dir):
        """Verify all ruleKeys use 'codescan:' prefix (not 'javacodescan:')."""
        request = {
            "files": [str(testdata_dir / "Sample.java")],
            "annotations": ["TODO", "FIXME"]
        }

        response = run_codescan(request)

        for issue in response["issues"]:
            assert issue["ruleKey"].startswith("codescan:"), \
                f"Expected 'codescan:' prefix, got: {issue['ruleKey']}"
            assert not issue["ruleKey"].startswith("javacodescan:"), \
                f"Old 'javacodescan:' prefix found: {issue['ruleKey']}"


class TestCodeScanBridgeHelp:
    """Test bridge CLI help."""

    @pytest.mark.quick
    @pytest.mark.codescan
    def test_help_command(self, codescan_bridge, java_available):
        """Test that help command works."""
        result = subprocess.run(
            ["java", "-jar", str(codescan_bridge), "--help"],
            capture_output=True,
            text=True
        )

        assert result.returncode == 0
        assert "ptx-codescan-bridge" in result.stdout
        assert "analyze" in result.stdout

    @pytest.mark.quick
    @pytest.mark.codescan
    def test_version_command(self, codescan_bridge, java_available, project_root):
        """Test that the bridge version matches plugin.json."""
        plugin_version = json.loads((project_root / "plugin.json").read_text())["version"]
        result = subprocess.run(
            ["java", "-jar", str(codescan_bridge), "--version"],
            capture_output=True,
            text=True
        )

        assert result.returncode == 0
        assert plugin_version in result.stdout
