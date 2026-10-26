.PHONY: all build build-native test test-native coverage clean dev help

# Default target
all: build

# Build JAR (default, no GraalVM needed)
build:
	@echo "Building ptx-codeinfo JAR..."
	mvn clean package -DskipTests -q
	@echo "Done: target/ptx-codeinfo.jar"

# Build JAR and GraalVM native image (requires GRAALVM_HOME)
build-native:
	@echo "Building ptx-codeinfo native image..."
	mvn clean package -DskipTests -Pnative -q
	@echo "Done: target/ptx-codeinfo"

# Run unit tests
test:
	@echo "Running codeinfo tests..."
	mvn test

# Compare native binary with JAR (run after build-native)
test-native:
	@echo "Running codeinfo native image tests..."
	cd .. && ./scripts/test-codeinfo-native.sh

# Run tests with coverage report and 95% line coverage gate
coverage:
	@echo "Running codeinfo tests with coverage..."
	mvn verify
	@echo "Report: target/site/jacoco/index.html"

# Clean build artifacts
clean:
	@echo "Cleaning..."
	mvn clean -q
	@echo "Done"

# Development build (quick, no tests)
dev: build

# Help
help:
	@echo "ptx-codeinfo - Build System"
	@echo ""
	@echo "Available targets:"
	@echo "  build        - Build JAR (default)"
	@echo "  build-native - Build JAR and GraalVM native image (requires GRAALVM_HOME)"
	@echo "  test         - Run unit tests"
	@echo "  test-native  - Compare native binary with JAR (run after build-native)"
	@echo "  coverage     - Run tests with coverage gate (95%)"
	@echo "  clean        - Remove build artifacts"
	@echo "  dev          - Quick development build"
	@echo "  help         - Show this help"
	@echo ""
	@echo "Distribution targets are in the root Makefile (dist-codeinfo, dist-codeinfo-native)"
