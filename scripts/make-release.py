#!/usr/bin/env python3
"""
Make Release Script for Portunix Dream Code Plugins

Usage:
    uv run python scripts/make-release.py <plugin-name> [version] [--os OS] [--arch ARCH]
    uv run python scripts/make-release.py codeanalysis 1.0.0
    uv run python scripts/make-release.py codeanalysis --os linux --arch amd64
    uv run python scripts/make-release.py codeanalysis  # auto-detect version from plugin.json

This script:
1. Validates the plugin and version (or auto-detects from plugin.json)
2. Creates dist/<plugin>/ directory
3. Copies binary and bridge JARs with versioning
4. Creates archives (.tar.gz, .zip)
5. Generates SHA256 checksums
6. Creates release notes
"""

import argparse
import hashlib
import json
import os
import platform
import shutil
import sys
import tarfile
import zipfile
from datetime import datetime, timezone
from pathlib import Path


class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    CYAN = '\033[0;36m'
    NC = '\033[0m'

    @classmethod
    def disable(cls):
        cls.RED = cls.GREEN = cls.YELLOW = cls.BLUE = cls.CYAN = cls.NC = ''


def print_header() -> None:
    print(f"{Colors.BLUE}========================================")
    print(f"  Portunix Dream Code Release Maker")
    print(f"========================================{Colors.NC}")
    print()


def print_step(message: str) -> None:
    print(f"{Colors.GREEN}>> {message}{Colors.NC}")


def print_info(message: str) -> None:
    print(f"{Colors.CYAN}   {message}{Colors.NC}")


def print_warning(message: str) -> None:
    print(f"{Colors.YELLOW}WARNING: {message}{Colors.NC}")


def print_error(message: str) -> None:
    print(f"{Colors.RED}ERROR: {message}{Colors.NC}")


def get_project_root() -> Path:
    script_dir = Path(__file__).parent.resolve()
    return script_dir.parent


def detect_os() -> str:
    system = platform.system().lower()
    if system == 'linux':
        return 'linux'
    elif system == 'darwin':
        return 'darwin'
    elif system == 'windows':
        return 'windows'
    return system


def detect_arch() -> str:
    machine = platform.machine().lower()
    if machine in ('x86_64', 'amd64'):
        return 'amd64'
    elif machine in ('aarch64', 'arm64'):
        return 'arm64'
    return machine


def detect_version_from_plugin_json(plugin_dir: Path) -> str | None:
    plugin_json = plugin_dir / "plugin.json"
    if not plugin_json.exists():
        return None

    try:
        content = json.loads(plugin_json.read_text(encoding='utf-8'))
        return content.get('version')
    except (json.JSONDecodeError, KeyError):
        return None


def calculate_sha256(file_path: Path) -> str:
    sha256_hash = hashlib.sha256()
    with open(file_path, 'rb') as f:
        for chunk in iter(lambda: f.read(4096), b''):
            sha256_hash.update(chunk)
    return sha256_hash.hexdigest()


def create_codeanalysis_release(version: str, target_os: str, target_arch: str) -> bool:
    """Create release for codeanalysis hybrid plugin (Go + Java bridges)"""
    project_root = get_project_root()
    plugin_dir = project_root / "codeanalysis"

    # Determine binary name based on OS (simple name without version/os/arch)
    binary_ext = ".exe" if target_os == "windows" else ""
    binary_name = f"ptx-codeanalysis{binary_ext}"

    # Check binary exists
    binary_path = plugin_dir / binary_name
    if not binary_path.exists():
        print_error(f"Binary not found: {binary_path}")
        print_info("Run 'make build-codeanalysis' first")
        return False

    # Check bridge JARs exist
    sonarlint_jar = plugin_dir / "bridges" / "sonarlint" / "target" / "ptx-sonarlint-bridge.jar"
    codescan_jar = plugin_dir / "bridges" / "codescan" / "target" / "ptx-codescan-bridge.jar"

    if not sonarlint_jar.exists():
        print_error(f"SonarLint bridge JAR not found: {sonarlint_jar}")
        print_info("Run 'make build-codeanalysis' first")
        return False

    if not codescan_jar.exists():
        print_error(f"CodeScan bridge JAR not found: {codescan_jar}")
        print_info("Run 'make build-codeanalysis' first")
        return False

    # Create dist directory
    dist_dir = project_root / "dist"
    plugin_dist = dist_dir / "ptx-codeanalysis"

    # Clean previous dist for this version/platform
    if plugin_dist.exists():
        shutil.rmtree(plugin_dist)
    plugin_dist.mkdir(parents=True, exist_ok=True)

    print_step("Creating distribution package...")

    # Copy binary with simple name (no version/os/arch in filename)
    dest_binary = plugin_dist / binary_name
    shutil.copy2(binary_path, dest_binary)
    # Make executable on Unix
    if target_os != "windows":
        dest_binary.chmod(0o755)
    print_info(f"Copied binary: {binary_name}")

    # Create bridges directory and copy JARs
    bridges_dist = plugin_dist / "bridges"
    bridges_dist.mkdir(parents=True, exist_ok=True)

    shutil.copy2(sonarlint_jar, bridges_dist / "ptx-sonarlint-bridge.jar")
    print_info("Copied: bridges/ptx-sonarlint-bridge.jar")

    shutil.copy2(codescan_jar, bridges_dist / "ptx-codescan-bridge.jar")
    print_info("Copied: bridges/ptx-codescan-bridge.jar")

    # Copy README
    readme_src = plugin_dir / "README.md"
    if readme_src.exists():
        shutil.copy2(readme_src, plugin_dist / "README.md")
        print_info("Copied: README.md")

    # Copy plugin.json
    plugin_json_src = plugin_dir / "plugin.json"
    if plugin_json_src.exists():
        shutil.copy2(plugin_json_src, plugin_dist / "plugin.json")
        print_info("Copied: plugin.json")

    # Copy defaults directory
    defaults_src = plugin_dir / "defaults"
    if defaults_src.exists():
        defaults_dist = plugin_dist / "defaults"
        shutil.copytree(defaults_src, defaults_dist)
        print_info("Copied: defaults/")

    # Create archives (stored in plugin_dist directory for cleaner organization)
    # Archive filename includes os/arch, directory inside uses just version
    print_step("Creating archives...")
    archive_filename = f"ptx-codeanalysis-{version}-{target_os}-{target_arch}"
    archive_dir = f"ptx-codeanalysis-{version}"

    # Create tar.gz (in plugin_dist, not root dist)
    tar_path = plugin_dist / f"{archive_filename}.tar.gz"
    with tarfile.open(tar_path, "w:gz") as tar:
        for item in plugin_dist.rglob("*"):
            if item.is_file() and not item.name.endswith(('.tar.gz', '.zip', '.txt', '.md')):
                arcname = f"{archive_dir}/{item.relative_to(plugin_dist)}"
                tar.add(item, arcname=arcname)
    print_info(f"Created: {tar_path.name}")

    # Create zip
    zip_path = plugin_dist / f"{archive_filename}.zip"
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for item in plugin_dist.rglob("*"):
            if item.is_file() and not item.name.endswith(('.tar.gz', '.zip', '.txt', '.md')):
                arcname = f"{archive_dir}/{item.relative_to(plugin_dist)}"
                zipf.write(item, arcname)
    print_info(f"Created: {zip_path.name}")

    # Generate checksums (underscore, stored in plugin_dist)
    print_step("Generating checksums...")
    checksums_path = plugin_dist / f"checksums_{version}.txt"
    with open(checksums_path, 'w') as f:
        for file_path in sorted(plugin_dist.glob("*.tar.gz")) + sorted(plugin_dist.glob("*.zip")):
            checksum = calculate_sha256(file_path)
            f.write(f"{checksum}  {file_path.name}\n")
    print_info(f"Created: {checksums_path.name}")

    # Create release notes (stored in plugin_dist)
    print_step("Creating release notes...")
    create_codeanalysis_release_notes(plugin_dist, version, target_os, target_arch)

    return True


def create_codeinfo_release(version: str, target_os: str, target_arch: str) -> bool:
    """Create release for codeinfo Java plugin (JAR or native binary)"""
    project_root = get_project_root()
    plugin_dir = project_root / "codeinfo"

    # Check for native binary first, fall back to JAR
    binary_ext = ".exe" if target_os == "windows" else ""
    native_binary = plugin_dir / "target" / f"ptx-codeinfo{binary_ext}"
    jar_path = plugin_dir / "target" / "ptx-codeinfo.jar"

    has_native = native_binary.exists()
    has_jar = jar_path.exists()

    if not has_native and not has_jar:
        print_error("No build artifacts found in codeinfo/target/")
        print_info("Run 'make build-codeinfo' or 'make build-codeinfo-native' first")
        return False

    # Create dist directory
    dist_dir = project_root / "dist"
    plugin_dist = dist_dir / "ptx-codeinfo"

    # Clean previous dist
    if plugin_dist.exists():
        shutil.rmtree(plugin_dist)
    plugin_dist.mkdir(parents=True, exist_ok=True)

    print_step("Creating distribution package...")

    # Determine what to include
    if has_native:
        # Native binary distribution (platform-specific)
        dest_binary = plugin_dist / f"ptx-codeinfo{binary_ext}"
        shutil.copy2(native_binary, dest_binary)
        if target_os != "windows":
            dest_binary.chmod(0o755)
        print_info(f"Copied native binary: ptx-codeinfo{binary_ext}")
        is_native_dist = True
    else:
        # JAR distribution (platform-independent), name must match the fallback in plugin.json
        dest_jar = plugin_dist / "ptx-codeinfo.jar"
        shutil.copy2(jar_path, dest_jar)
        print_info("Copied JAR: ptx-codeinfo.jar")
        is_native_dist = False

    # Copy README
    readme_src = plugin_dir / "README.md"
    if readme_src.exists():
        shutil.copy2(readme_src, plugin_dist / "README.md")
        print_info("Copied: README.md")

    # Copy plugin.json
    plugin_json_src = plugin_dir / "plugin.json"
    if plugin_json_src.exists():
        shutil.copy2(plugin_json_src, plugin_dist / "plugin.json")
        print_info("Copied: plugin.json")

    # Create archives (stored in plugin_dist for cleaner organization)
    print_step("Creating archives...")

    if is_native_dist:
        # Native: include OS/arch in archive name
        archive_filename = f"ptx-codeinfo-{version}-{target_os}-{target_arch}"
    else:
        # JAR: platform-independent
        archive_filename = f"ptx-codeinfo-{version}"

    archive_dir = f"ptx-codeinfo-{version}"

    # Create tar.gz (in plugin_dist)
    tar_path = plugin_dist / f"{archive_filename}.tar.gz"
    with tarfile.open(tar_path, "w:gz") as tar:
        for item in plugin_dist.rglob("*"):
            if item.is_file() and not item.name.endswith(('.tar.gz', '.zip', '.txt', '.md')):
                arcname = f"{archive_dir}/{item.relative_to(plugin_dist)}"
                tar.add(item, arcname=arcname)
    print_info(f"Created: {tar_path.name}")

    # Create zip
    zip_path = plugin_dist / f"{archive_filename}.zip"
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for item in plugin_dist.rglob("*"):
            if item.is_file() and not item.name.endswith(('.tar.gz', '.zip', '.txt', '.md')):
                arcname = f"{archive_dir}/{item.relative_to(plugin_dist)}"
                zipf.write(item, arcname)
    print_info(f"Created: {zip_path.name}")

    # Generate checksums (underscore, stored in plugin_dist)
    print_step("Generating checksums...")
    checksums_path = plugin_dist / f"checksums_{version}.txt"
    with open(checksums_path, 'w') as f:
        for file_path in sorted(plugin_dist.glob("*.tar.gz")) + sorted(plugin_dist.glob("*.zip")):
            checksum = calculate_sha256(file_path)
            f.write(f"{checksum}  {file_path.name}\n")
    print_info(f"Created: {checksums_path.name}")

    # Create release notes (stored in plugin_dist)
    print_step("Creating release notes...")
    create_codeinfo_release_notes(plugin_dist, version, target_os, target_arch, is_native_dist)

    return True


def create_codeinfo_release_notes(dist_dir: Path, version: str, target_os: str, target_arch: str, is_native: bool) -> None:
    build_date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    if is_native:
        binary_info = f"ptx-codeinfo (native binary for {target_os}/{target_arch})"
        run_cmd = "./ptx-codeinfo"
        platform_line = f"**Platform:** {target_os}/{target_arch}"
    else:
        binary_info = "ptx-codeinfo.jar (requires Java 21+)"
        run_cmd = "java -jar ptx-codeinfo.jar"
        platform_line = "**Platform:** All (requires Java 21+)"

    notes = f"""# ptx-codeinfo v{version}

Fast code analysis tool powered by GraalVM Native Image.

## Features

- Fast startup (<50ms with native binary)
- Lines of code counter for Java source files
- Verbose output with detailed breakdown

## Requirements

{"- No runtime dependencies (native binary)" if is_native else "- Java 21 or higher"}

## Installation

1. Extract the archive to your preferred location
2. Add to PATH or run directly

## Usage

```bash
# Count lines of code
{run_cmd} loc Example.java

# Verbose output
{run_cmd} loc Example.java --verbose
```

## Package Contents

```
ptx-codeinfo-{version}/
├── {binary_info.split()[0]}
├── plugin.json
└── README.md
```

---

**Build Date:** {build_date}
{platform_line}
**Repository:** https://github.com/CassandraGargoyle/portunix-dream-code
"""

    notes_path = dist_dir / f"RELEASE_NOTES_{version}.md"
    notes_path.write_text(notes)
    print_info(f"Created: {notes_path.name}")


def create_codeanalysis_release_notes(dist_dir: Path, version: str, target_os: str, target_arch: str) -> None:
    build_date = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    binary_ext = ".exe" if target_os == "windows" else ""

    notes = f"""# ptx-codeanalysis v{version}

Code analysis plugin integrating SonarLint and CodeScan engines for Portunix.

## Features

- Static code analysis with SonarLint engine
- Code annotation detection (TODO, FIXME, HACK, XXX) with JavaCodeScan
- YAML-based configuration (.ptxcode files)
- Multiple output formats (console, JSON, SARIF, HTML)
- CI/CD integration with threshold-based exit codes

## Supported Languages

- Java
- JavaScript / TypeScript
- Python
- Go (limited)
- Kotlin (limited)

## Requirements

- Java 17 or higher (for analysis bridges)
- Portunix 1.10.0 or higher

## Installation

1. Extract the archive to your preferred location
2. Ensure Java 17+ is available in PATH
3. Register with Portunix or run standalone

## Usage

```bash
# Run analysis on current directory
./ptx-codeanalysis analyze

# Initialize configuration
./ptx-codeanalysis init --template java

# Validate configuration
./ptx-codeanalysis validate .ptxcode

# List available rules
./ptx-codeanalysis rules --engine sonarlint
```

## Package Contents

```
ptx-codeanalysis-{version}/
├── ptx-codeanalysis{binary_ext}             # Main binary
├── bridges/
│   ├── ptx-sonarlint-bridge.jar      # SonarLint analysis engine
│   └── ptx-codescan-bridge.jar       # Code annotation scanner
├── defaults/
│   └── sample.ptxcode                # Sample configuration
├── plugin.json                       # Plugin manifest
└── README.md                         # Documentation
```

## Verification

Verify download using SHA256 checksums in `checksums-{version}-{target_os}-{target_arch}.txt`.

---

**Build Date:** {build_date}
**Platform:** {target_os}/{target_arch}
**Repository:** https://github.com/CassandraGargoyle/portunix-dream-code
"""

    notes_path = dist_dir / f"RELEASE_NOTES_{version}.md"
    notes_path.write_text(notes)
    print_info(f"Created: {notes_path.name}")


def show_summary(plugin_name: str, version: str, target_os: str, target_arch: str) -> None:
    project_root = get_project_root()
    dist_dir = project_root / "dist"
    plugin_dist = dist_dir / f"ptx-{plugin_name}"
    binary_ext = ".exe" if target_os == "windows" else ""

    print()
    print(f"{Colors.GREEN}========================================")
    print(f"  Release {version} ({target_os}/{target_arch}) ready!")
    print(f"========================================{Colors.NC}")
    print()
    print(f"Distribution: dist/ptx-{plugin_name}/")
    print()
    if plugin_dist.exists():
        # Show archives and metadata
        print("Archives:")
        for f in sorted(plugin_dist.glob("*.tar.gz")) + sorted(plugin_dist.glob("*.zip")):
            size_mb = f.stat().st_size / (1024 * 1024)
            print(f"  {f.name} ({size_mb:.1f} MB)")
        print()
        print("Metadata:")
        for f in sorted(plugin_dist.glob("*.txt")) + sorted(plugin_dist.glob("*.md")):
            print(f"  {f.name}")
        print()
        print("Package contents:")
        for f in sorted(plugin_dist.rglob("*")):
            if f.is_file() and not f.name.endswith(('.tar.gz', '.zip', '.txt', '.md')):
                rel_path = f.relative_to(plugin_dist)
                size_mb = f.stat().st_size / (1024 * 1024)
                print(f"  {rel_path} ({size_mb:.1f} MB)")
    print()
    print("Next steps:")
    print(f"  1. Test: dist/ptx-{plugin_name}/ptx-{plugin_name}{binary_ext} --help")
    print(f"  2. Upload archives from dist/ptx-{plugin_name}/ to GitHub release")
    print()


def show_usage() -> None:
    print("Usage: python scripts/make-release.py <plugin-name> [version] [--os OS] [--arch ARCH]")
    print()
    print("Arguments:")
    print("  plugin-name   Name of the plugin to package")
    print("  version       Version string (auto-detected from plugin.json if not provided)")
    print()
    print("Options:")
    print("  --os OS       Target OS: linux, windows, darwin (default: current)")
    print("  --arch ARCH   Target architecture: amd64, arm64 (default: current)")
    print()
    print("Examples:")
    print("  python scripts/make-release.py codeanalysis 1.0.0")
    print("  python scripts/make-release.py codeanalysis --os linux --arch amd64")
    print("  python scripts/make-release.py codeanalysis  # auto-detect version")
    print()
    print("Available plugins:")
    print("  codeanalysis  - Static code analysis (Go + Java bridges)")
    print("  codeinfo      - Code metrics tool (Java/GraalVM native)")


def main() -> int:
    if not sys.stdout.isatty():
        Colors.disable()

    print_header()

    parser = argparse.ArgumentParser(
        description="Create release packages for Portunix Dream Code plugins",
        add_help=False
    )
    parser.add_argument('plugin_name', nargs='?', help='Plugin name')
    parser.add_argument('version', nargs='?', help='Version string')
    parser.add_argument('--os', dest='target_os', help='Target OS')
    parser.add_argument('--arch', dest='target_arch', help='Target architecture')
    parser.add_argument('-h', '--help', action='store_true', help='Show help')

    args = parser.parse_args()

    if args.help or not args.plugin_name:
        show_usage()
        return 0 if args.help else 1

    plugin_name = args.plugin_name
    project_root = get_project_root()

    # Determine target OS and architecture
    target_os = args.target_os or detect_os()
    target_arch = args.target_arch or detect_arch()

    # Validate OS and arch
    valid_os = ['linux', 'windows', 'darwin']
    valid_arch = ['amd64', 'arm64']

    if target_os not in valid_os:
        print_error(f"Invalid OS: {target_os}. Valid options: {', '.join(valid_os)}")
        return 1

    if target_arch not in valid_arch:
        print_error(f"Invalid architecture: {target_arch}. Valid options: {', '.join(valid_arch)}")
        return 1

    # Get plugin directory
    plugin_dir = project_root / plugin_name
    if not plugin_dir.exists():
        print_error(f"Plugin directory not found: {plugin_dir}")
        show_usage()
        return 1

    # Get version from argument or auto-detect
    version = args.version
    if not version:
        print_info("Auto-detecting version from plugin.json...")
        version = detect_version_from_plugin_json(plugin_dir)
        if not version:
            print_error(f"Could not detect version for {plugin_name}")
            print_info("Please provide version: python scripts/make-release.py codeanalysis 1.0.0")
            return 1
        print_info(f"Detected version: {version}")

    print_info(f"Creating release for {plugin_name} v{version} ({target_os}/{target_arch})")
    print()

    # Route to appropriate plugin handler
    if plugin_name == "codeanalysis":
        if not create_codeanalysis_release(version, target_os, target_arch):
            return 1
    elif plugin_name == "codeinfo":
        if not create_codeinfo_release(version, target_os, target_arch):
            return 1
    else:
        print_error(f"Unknown plugin: {plugin_name}")
        print_info("Currently supported: codeanalysis, codeinfo")
        return 1

    show_summary(plugin_name, version, target_os, target_arch)
    return 0


if __name__ == "__main__":
    sys.exit(main())
