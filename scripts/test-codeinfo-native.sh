#!/bin/bash

# Compare ptx-codeinfo native binary output with the JAR on all test fixtures
# Usage: ./scripts/test-codeinfo-native.sh
# Requires: make build-codeinfo-native (produces both the binary and the JAR)

TARGET_DIR="codeinfo/target"
FIXTURES_DIR="codeinfo/src/test/resources/fixtures"
NATIVE="$TARGET_DIR/ptx-codeinfo"
JAR=$(ls "$TARGET_DIR"/dream-code-codeinfo-*.jar 2>/dev/null | grep -v original- | head -1)

if [ ! -x "$NATIVE" ] || [ -z "$JAR" ]; then
    echo "Error: native binary or JAR not found, run 'make build-codeinfo-native' first"
    exit 1
fi

FAILED=0

# Run a case with both binaries and compare stdout, stderr and exit code
check() {
    local name="$1"
    shift
    local native_out jar_out
    native_out=$("$NATIVE" "$@" 2>&1; echo "exit=$?")
    jar_out=$(java -jar "$JAR" "$@" 2>&1; echo "exit=$?")

    if [ "$native_out" = "$jar_out" ]; then
        echo "  ✓ $name"
    else
        echo "  ✗ $name"
        echo "    native: $native_out"
        echo "    jar:    $jar_out"
        FAILED=1
    fi
}

echo "Comparing native binary with JAR..."

for FIXTURE in "$FIXTURES_DIR"/*.java; do
    check "loc -v $(basename "$FIXTURE")" loc -v "$FIXTURE"
done

check "loc missing file" loc "$FIXTURES_DIR/DoesNotExist.java"
check "loc non-java file" loc "$0"
check "loc without file" loc
check "--version" --version
check "--help-ai" --help-ai

if [ $FAILED -ne 0 ]; then
    echo "Native binary output differs from JAR"
    exit 1
fi

echo "Native binary matches JAR"
