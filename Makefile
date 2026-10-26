.PHONY: all build build-java build-java-native build-codeinfo build-codeinfo-native build-codeanalysis clean test test-java test-codeinfo test-codeinfo-native test-codeanalysis test-quick test-codescan deps dev loc loc-all loc-file dist dist-codeanalysis dist-codeinfo dist-codeinfo-native release deploy-local undeploy-local help

# Default target
all: build

# Build everything
build: build-java build-codeanalysis

# Build Java modules (Maven) - skip native image
build-java:
	@echo "Building Java modules..."
	mvn clean package -DskipTests -DskipNativeBuild=true -q
	@echo "Done: Java modules"

# Build Java modules with GraalVM native image (requires GRAALVM_HOME)
build-java-native:
	@echo "Building Java modules with native image..."
	mvn clean package -DskipTests -q
	@echo "Done: Java modules (native)"

# Build codeinfo JAR only
build-codeinfo:
	@echo "Building codeinfo..."
	mvn clean package -DskipTests -DskipNativeBuild=true -q -pl codeinfo
	@echo "Done: codeinfo/target/dream-code-codeinfo-*.jar"

# Build codeinfo with GraalVM native image (requires GRAALVM_HOME)
build-codeinfo-native:
	@echo "Building codeinfo native image..."
	mvn clean package -DskipTests -q -pl codeinfo
	@echo "Done: codeinfo/target/ptx-codeinfo"

# Build codeanalysis (Go + Java bridges)
build-codeanalysis:
	@echo "Building codeanalysis module..."
	$(MAKE) -C codeanalysis build
	@echo "Done: codeanalysis"

# Run all tests
test: test-java test-codeanalysis

# Run Java tests
test-java:
	@echo "Running Java tests..."
	mvn test

# Run codeinfo tests
test-codeinfo:
	@echo "Running codeinfo tests..."
	mvn test -pl codeinfo

# Compare codeinfo native binary with JAR (run after build-codeinfo-native)
test-codeinfo-native:
	@echo "Running codeinfo native image tests..."
	./scripts/test-codeinfo-native.sh

# Run codeanalysis tests
test-codeanalysis:
	@echo "Running codeanalysis tests..."
	$(MAKE) -C codeanalysis test

# Quick tests (codeanalysis only)
test-quick:
	@echo "Running quick tests..."
	$(MAKE) -C codeanalysis test-quick

# CodeScan tests only
test-codescan:
	@echo "Running CodeScan tests..."
	$(MAKE) -C codeanalysis test-codescan

# Clean all build artifacts
clean:
	@echo "Cleaning Java modules..."
	mvn clean -q
	@echo "Cleaning codeanalysis..."
	$(MAKE) -C codeanalysis clean
	@echo "Done"

# Install dependencies
deps:
	@echo "Installing Java dependencies..."
	mvn dependency:resolve -q
	@echo "Installing codeanalysis dependencies..."
	$(MAKE) -C codeanalysis deps
	@echo "Done"

# Quick development build (skip tests)
dev: build-java
	$(MAKE) -C codeanalysis dev

# Run codeinfo LOC analysis on project's Java source files
loc:
	@echo "Analyzing lines of code in project..."
	@find . -path '*/target' -prune -o -type f -name "*.java" -print | while read f; do \
		mvn -q -pl codeinfo exec:java -Dexec.mainClass="org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI" -Dexec.args="loc $$f" 2>/dev/null; \
	done | sort -t' ' -k1 -rn | head -20
	@echo ""
	@echo "Top 20 files by LOC (use 'make loc-all' for full list)"

# Run codeinfo LOC analysis on all Java files (full output)
loc-all:
	@echo "Analyzing lines of code in project (all files)..."
	@find . -path '*/target' -prune -o -type f -name "*.java" -print | while read f; do \
		mvn -q -pl codeinfo exec:java -Dexec.mainClass="org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI" -Dexec.args="loc $$f" 2>/dev/null; \
	done | sort -t' ' -k1 -rn

# Run codeinfo LOC with verbose output on specific file
# Usage: make loc-file FILE=path/to/File.java
loc-file:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make loc-file FILE=path/to/File.java"; \
		exit 1; \
	fi
	@mvn -q -pl codeinfo exec:java -Dexec.mainClass="org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI" -Dexec.args="loc $(FILE) --verbose"

# Distribution targets
# Python environment managed by uv (shared with codeanalysis, see pyproject.toml)
UV_RUN := uv run --project $(CURDIR)

setup-venv:
	@if ! command -v uv >/dev/null 2>&1; then \
		echo "Error: uv not found in PATH (install: https://docs.astral.sh/uv/)"; \
		exit 1; \
	fi
	@uv sync --project $(CURDIR) --quiet

dist: dist-codeanalysis dist-codeinfo

dist-codeanalysis:
	@echo "Creating codeanalysis distribution..."
	$(MAKE) -C codeanalysis dist
	@echo "Done: dist/ptx-codeanalysis/"

dist-codeinfo: build-codeinfo setup-venv
	@echo "Creating codeinfo distribution (JAR)..."
	$(UV_RUN) python scripts/make-release.py codeinfo
	@echo "Done: dist/ptx-codeinfo/"

dist-codeinfo-native: build-codeinfo-native setup-venv
	@echo "Creating codeinfo distribution (native binary)..."
	$(UV_RUN) python scripts/make-release.py codeinfo
	@echo "Done: dist/ptx-codeinfo/"

release: test dist
	@echo "Release packages created with tests passed"

# Local deployment - install plugins to local portunix
deploy-local: dist
	@echo "Deploying plugins to local Portunix..."
	@if ! command -v portunix >/dev/null 2>&1; then \
		echo "Error: portunix not found in PATH"; \
		exit 1; \
	fi
	@echo "Installing codeanalysis plugin..."
	@portunix plugin install dist/ptx-codeanalysis || echo "Warning: codeanalysis install failed"
	@echo "Installing codeinfo plugin..."
	@portunix plugin install dist/ptx-codeinfo || echo "Warning: codeinfo install failed"
	@echo ""
	@echo "Deployed plugins:"
	@portunix plugin list | grep -E "codeanalysis|codeinfo" || true
	@echo ""
	@echo "Done. Use 'portunix plugin list' to verify installation."

undeploy-local:
	@echo "Removing plugins from local Portunix..."
	@if ! command -v portunix >/dev/null 2>&1; then \
		echo "Error: portunix not found in PATH"; \
		exit 1; \
	fi
	@portunix plugin uninstall code-analysis -f 2>/dev/null || echo "code-analysis not installed"
	@portunix plugin uninstall code-info -f 2>/dev/null || echo "code-info not installed"
	@echo "Done."

# Help
help:
	@echo "Portunix Dream Code - Build System"
	@echo ""
	@echo "Available targets:"
	@echo "  all                - Build everything (default)"
	@echo "  build              - Build Java modules and codeanalysis"
	@echo "  build-java         - Build Java modules (skip native image)"
	@echo "  build-java-native  - Build Java modules with GraalVM native image"
	@echo "  build-codeinfo     - Build codeinfo JAR only"
	@echo "  build-codeinfo-native - Build codeinfo with GraalVM native image"
	@echo "  build-codeanalysis - Build codeanalysis module (Go + Java)"
	@echo "  test               - Run all tests"
	@echo "  test-java          - Run Java tests only"
	@echo "  test-codeinfo      - Run codeinfo tests only"
	@echo "  test-codeinfo-native - Compare codeinfo native binary with JAR"
	@echo "  test-codeanalysis  - Run codeanalysis tests only"
	@echo "  test-quick         - Run quick tests (codeanalysis)"
	@echo "  test-codescan      - Run CodeScan tests only"
	@echo "  clean              - Remove all build artifacts"
	@echo "  deps               - Install all dependencies"
	@echo "  dev                - Quick development build"
	@echo ""
	@echo "Code analysis:"
	@echo "  loc                - LOC analysis (top 20 Java files)"
	@echo "  loc-all            - LOC analysis (all Java files)"
	@echo "  loc-file FILE=...  - LOC analysis for specific file (verbose)"
	@echo ""
	@echo "Distribution targets:"
	@echo "  setup-venv         - Sync Python environment with uv (.venv/)"
	@echo "  dist               - Create all distribution packages"
	@echo "  dist-codeanalysis  - Create codeanalysis distribution (native)"
	@echo "  dist-codeinfo      - Create codeinfo distribution (JAR)"
	@echo "  dist-codeinfo-native - Create codeinfo distribution (native, requires GraalVM)"
	@echo "  release            - Run tests and create distributions"
	@echo ""
	@echo "Local deployment:"
	@echo "  deploy-local       - Install plugins to local Portunix"
	@echo "  undeploy-local     - Remove plugins from local Portunix"
	@echo ""
	@echo "  help               - Show this help"
	@echo ""
	@echo "Submodule targets (via codeanalysis/Makefile):"
	@echo "  make -C codeanalysis dist-all   - Build for all platforms"
	@echo "  make -C codeanalysis <target>"
