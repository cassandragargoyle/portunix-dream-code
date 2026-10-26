#!/usr/bin/env python3
"""
Simple test runner for ptx-codeanalysis.

Usage:
    uv run python test/run-tests.py              # Run all tests
    uv run python test/run-tests.py --quick      # Run quick tests only
    uv run python test/run-tests.py --codescan   # Run CodeScan tests only
    uv run python test/run-tests.py -v           # Verbose output
"""

import sys
import subprocess
import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(
        description="ptx-codeanalysis Test Runner",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    %(prog)s                    # Run all tests
    %(prog)s --quick            # Run quick tests only
    %(prog)s --codescan         # Run CodeScan tests
    %(prog)s --sonarlint        # Run SonarLint tests
    %(prog)s -v                 # Verbose output
    %(prog)s --coverage         # With coverage report
        """
    )

    parser.add_argument("-q", "--quick", action="store_true",
                        help="Run quick tests only")
    parser.add_argument("--codescan", action="store_true",
                        help="Run CodeScan tests only")
    parser.add_argument("--sonarlint", action="store_true",
                        help="Run SonarLint tests only")
    parser.add_argument("-v", "--verbose", action="store_true",
                        help="Verbose output")
    parser.add_argument("--coverage", action="store_true",
                        help="Generate coverage report")
    parser.add_argument("-k", "--filter",
                        help="Filter tests by name pattern")

    args = parser.parse_args()

    # Determine paths
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    test_dir = script_dir

    # Build pytest command
    cmd = [sys.executable, "-m", "pytest", str(test_dir)]

    # Verbosity
    if args.verbose:
        cmd.append("-v")
    else:
        cmd.append("-q")

    # Test filtering
    markers = []
    if args.quick:
        markers.append("quick")
    if args.codescan:
        markers.append("codescan")
    if args.sonarlint:
        markers.append("sonarlint")

    if markers:
        cmd.extend(["-m", " or ".join(markers)])

    if args.filter:
        cmd.extend(["-k", args.filter])

    # Coverage
    if args.coverage:
        cmd.extend(["--cov=.", "--cov-report=term-missing"])

    # Print header
    print("=" * 60)
    print("  ptx-codeanalysis Test Suite")
    print("=" * 60)

    if args.quick:
        print("Mode: Quick tests")
    elif args.codescan:
        print("Mode: CodeScan tests")
    elif args.sonarlint:
        print("Mode: SonarLint tests")
    else:
        print("Mode: All tests")

    print("=" * 60)
    print()

    # Run tests
    try:
        result = subprocess.run(cmd, cwd=project_root)
        return result.returncode
    except KeyboardInterrupt:
        print("\nTest execution interrupted")
        return 130
    except Exception as e:
        print(f"Error running tests: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
