.PHONY: all build build-go build-bridge clean test test-quick test-codescan test-integration deps dev dist dist-linux-amd64 dist-linux-arm64 dist-windows-amd64 dist-darwin-amd64 dist-darwin-arm64 dist-all setup-venv help

# Python environment managed by uv (pyproject.toml in project root)
PROJECT_ROOT := $(shell cd .. && pwd)
UV_RUN := uv run --project $(PROJECT_ROOT)

# Default target
all: build

# Build everything
build: build-go build-bridge

# Build Go binary
build-go:
	@echo "Building ptx-codeanalysis..."
	cd cmd/ptx-codeanalysis && go build -o ../../ptx-codeanalysis .
	@echo "Done: ptx-codeanalysis"

# Build Java bridges (delegate to bridges/Makefile)
build-bridge:
	$(MAKE) -C bridges build

# Run all tests (Python integration tests)
test: setup-venv
	@echo "Running integration tests..."
	$(UV_RUN) pytest test/ -v

# Quick tests only
test-quick: setup-venv
	@echo "Running quick tests..."
	$(UV_RUN) pytest test/ -v -m quick

# CodeScan tests only
test-codescan: setup-venv
	@echo "Running CodeScan tests..."
	$(UV_RUN) pytest test/ -v -m codescan

# Full integration tests
test-integration: setup-venv
	@echo "Running full integration tests..."
	$(UV_RUN) pytest test/integration/ -v

# Clean build artifacts
clean:
	@echo "Cleaning..."
	rm -f ptx-codeanalysis ptx-codeanalysis.exe
	$(MAKE) -C bridges clean
	rm -rf test/__pycache__ test/.pytest_cache
	@echo "Done"

# Install dependencies
deps: setup-venv
	go mod download
	go mod tidy

# Development build (quick, no tests)
dev: build-go

# Distribution targets
VERSION ?= $(shell python3 -c "import json; print(json.load(open('plugin.json'))['version'])" 2>/dev/null || echo "0.0.0")

# Sync Python environment with uv
setup-venv:
	@if ! command -v uv >/dev/null 2>&1; then \
		echo "Error: uv not found in PATH (install: https://docs.astral.sh/uv/)"; \
		exit 1; \
	fi
	@uv sync --project $(PROJECT_ROOT) --quiet

dist: build setup-venv
	@echo "Creating distribution package for current platform..."
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION)

dist-linux-amd64: build-bridge setup-venv
	@echo "Building for Linux AMD64..."
	GOOS=linux GOARCH=amd64 go build -o ptx-codeanalysis ./cmd/ptx-codeanalysis
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION) --os linux --arch amd64

dist-linux-arm64: build-bridge setup-venv
	@echo "Building for Linux ARM64..."
	GOOS=linux GOARCH=arm64 go build -o ptx-codeanalysis ./cmd/ptx-codeanalysis
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION) --os linux --arch arm64

dist-windows-amd64: build-bridge setup-venv
	@echo "Building for Windows AMD64..."
	GOOS=windows GOARCH=amd64 go build -o ptx-codeanalysis.exe ./cmd/ptx-codeanalysis
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION) --os windows --arch amd64

dist-darwin-amd64: build-bridge setup-venv
	@echo "Building for macOS AMD64..."
	GOOS=darwin GOARCH=amd64 go build -o ptx-codeanalysis ./cmd/ptx-codeanalysis
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION) --os darwin --arch amd64

dist-darwin-arm64: build-bridge setup-venv
	@echo "Building for macOS ARM64..."
	GOOS=darwin GOARCH=arm64 go build -o ptx-codeanalysis ./cmd/ptx-codeanalysis
	cd .. && $(UV_RUN) python scripts/make-release.py codeanalysis $(VERSION) --os darwin --arch arm64

dist-all: build-bridge setup-venv
	@echo "Building for all platforms..."
	$(MAKE) dist-linux-amd64
	$(MAKE) dist-linux-arm64
	$(MAKE) dist-windows-amd64
	$(MAKE) dist-darwin-amd64
	$(MAKE) dist-darwin-arm64
	@echo "All distributions created in ../dist/"

# Help
help:
	@echo "Available targets:"
	@echo "  all              - Build everything (default)"
	@echo "  build            - Build Go binary and Java bridges"
	@echo "  build-go         - Build only Go binary"
	@echo "  build-bridge     - Build all Java bridges"
	@echo ""
	@echo "Testing:"
	@echo "  test             - Run all tests"
	@echo "  test-quick       - Run quick tests only"
	@echo "  test-codescan    - Run CodeScan tests only"
	@echo "  test-integration - Run full integration tests"
	@echo ""
	@echo "Other:"
	@echo "  clean            - Remove build artifacts"
	@echo "  deps             - Download dependencies"
	@echo "  dev              - Quick development build"
	@echo ""
	@echo "Distribution targets:"
	@echo "  setup-venv       - Sync Python environment with uv"
	@echo "  dist             - Create dist package for current platform"
	@echo "  dist-all         - Create dist for all platforms"
	@echo ""
	@echo "  help             - Show this help"
