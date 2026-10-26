.PHONY: all build build-sonarlint build-codescan clean test test-sonarlint test-codescan help

# Default target
all: build

# Build all bridges
build: build-sonarlint build-codescan

build-sonarlint:
	@echo "Building SonarLint bridge..."
	cd sonarlint && mvn clean package -DskipTests -q
	@echo "Done: sonarlint/target/ptx-sonarlint-bridge.jar"

build-codescan:
	@echo "Building CodeScan bridge..."
	cd codescan && mvn clean package -DskipTests -q
	@echo "Done: codescan/target/ptx-codescan-bridge.jar"

# Run tests
test: test-sonarlint test-codescan

test-sonarlint:
	@echo "Running SonarLint bridge tests..."
	cd sonarlint && mvn test

test-codescan:
	@echo "Running CodeScan bridge tests..."
	cd codescan && mvn test

# Clean build artifacts
clean:
	@echo "Cleaning bridges..."
	cd sonarlint && mvn clean -q
	cd codescan && mvn clean -q
	@echo "Done"

# Help
help:
	@echo "Available targets:"
	@echo "  all              - Build all bridges (default)"
	@echo "  build            - Build all bridges"
	@echo "  build-sonarlint  - Build SonarLint bridge only"
	@echo "  build-codescan   - Build CodeScan bridge only"
	@echo "  test             - Run all bridge tests"
	@echo "  test-sonarlint   - Run SonarLint tests"
	@echo "  test-codescan    - Run CodeScan tests"
	@echo "  clean            - Remove build artifacts"
	@echo "  help             - Show this help"
