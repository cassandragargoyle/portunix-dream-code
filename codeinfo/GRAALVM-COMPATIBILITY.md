# GraalVM Native Image Compatibility

This document describes the compatibility of ptx-codeinfo with GraalVM Native Image.

## GraalVM Edition Support

**✅ GraalVM Community Edition** - Fully Supported

- **License**: GPL 2 with Classpath Exception (Free)
- **Native Image**: Included in Community Edition
- **Platforms**: Linux x64, Windows x64, macOS aarch64 (Apple Silicon)
- **Recommendation**: Use GraalVM Community Edition 23.1.0 or later with Java 21

## Dependency Compatibility

### Picocli 4.7.7 - ✅ Excellent Support

Picocli has first-class support for GraalVM Native Image:

- **Reflection Configuration**: Automatic generation via `picocli-codegen`
- **Performance**: Sub-10ms startup (vs ~500ms on JVM)
- **Tools Provided**:
  - `ReflectionConfigGenerator` - Generates reflection config for @Command, @Option, @Parameters
  - `ResourceConfigGenerator` - Generates resource bundle config
  - `DynamicProxyConfigGenerator` - For @Command-annotated interfaces

**Build Configuration**: Already configured in `pom.xml` with annotation processor

### JavaParser 3.27.1 - ✅ Works Without Extra Configuration

JavaParser uses reflection in its language level validators (`PropertyMetaModel.getValue`).
In a native image these fail with `NoSuchFieldError` because the AST fields are not
registered for reflection.

ptx-codeinfo avoids this by parsing with `LanguageLevel.RAW`, which skips the validators.
LOC counting needs only the grammar and the lexer (comments), so no reflection metadata
for JavaParser is needed. The validators also rejected modern syntax such as text blocks
at the default language level.

If a future feature needs validators or other reflective JavaParser APIs, generate the
metadata with the GraalVM Tracing Agent:
```bash
# Run with tracing agent to collect metadata
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/ptx-codeinfo.jar loc Example.java
```

## Build Requirements

### For JAR Build (No GraalVM Required)
- Java 21 or later
- Maven 3.8+

### For Native Image Build
- GraalVM Community Edition 23.1.0+ with Java 21
- Native Image component installed
- Platform-specific requirements:
  - **Linux**: glibc-devel, zlib-devel, gcc
  - **Windows**: Visual Studio Build Tools with Windows SDK
  - **macOS**: Xcode Command Line Tools

## Installation Instructions

### Linux (Ubuntu/Debian)
```bash
# Download GraalVM Community Edition
curl -L https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.1/graalvm-community-jdk-21.0.1_linux-x64_bin.tar.gz -o graalvm.tar.gz

# Extract
tar -xzf graalvm.tar.gz
export GRAALVM_HOME=$PWD/graalvm-community-openjdk-21.0.1
export PATH=$GRAALVM_HOME/bin:$PATH

# Verify
java -version
native-image --version
```

### Windows
```powershell
# Download from https://github.com/graalvm/graalvm-ce-builds/releases
# Extract and set environment variables
$env:GRAALVM_HOME = "C:\path\to\graalvm-community-openjdk-21"
$env:PATH = "$env:GRAALVM_HOME\bin;$env:PATH"
```

### macOS (Apple Silicon)
```bash
# Download GraalVM for macOS aarch64
curl -L https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.1/graalvm-community-jdk-21.0.1_macos-aarch64_bin.tar.gz -o graalvm.tar.gz

# Extract and configure
tar -xzf graalvm.tar.gz
export GRAALVM_HOME=$PWD/graalvm-community-openjdk-21.0.1/Contents/Home
export PATH=$GRAALVM_HOME/bin:$PATH
```

## Building Native Image

### Step 1: Generate Reflection Metadata (First Time Only)

Run the application with the GraalVM Tracing Agent to collect metadata:

```bash
# Build JAR first
mvn clean package -DskipTests

# Run with tracing agent
mkdir -p src/main/resources/META-INF/native-image
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/ptx-codeinfo.jar loc src/test/resources/fixtures/SimpleClass.java

# Test with different inputs to capture all code paths
java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image \
     -jar target/ptx-codeinfo.jar loc src/test/resources/fixtures/MixedContent.java

java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image \
     -jar target/ptx-codeinfo.jar loc --verbose src/test/resources/fixtures/CommentOnlyFile.java
```

This generates:
- `reflect-config.json` - Reflection configuration
- `resource-config.json` - Resource bundle configuration
- `jni-config.json` - JNI configuration (if needed)
- `proxy-config.json` - Dynamic proxy configuration

### Step 2: Build Native Image

```bash
# Build native image with Maven
mvn clean package -Pnative

# Or build manually with native-image command
native-image -jar target/ptx-codeinfo.jar \
             --no-fallback \
             -H:+ReportExceptionStackTraces \
             ptx-codeinfo
```

### Step 3: Test Native Binary

```bash
# Test help
./ptx-codeinfo --help

# Test LOC command
./ptx-codeinfo loc Example.java

# Test verbose mode
./ptx-codeinfo loc --verbose Example.java
```

## Known Limitations

### Reflection Configuration for New Features

The current `loc` command needs no JavaParser reflection metadata (see above). A new
feature that uses reflective JavaParser APIs needs the tracing agent:

1. Run application with various inputs using tracing agent
2. Merge configurations from multiple runs
3. Manually verify/adjust generated configs if needed

### Platform-Specific Builds

GraalVM does not support cross-compilation:
- Build on Linux for Linux binary
- Build on Windows for Windows binary
- Build on macOS for macOS binary

### Binary Size

The Linux x64 binary is 22.7 MB with GraalVM CE 21.0.2 (target: < 20 MB). About half
is code (`java.base` 4.9 MB, JavaParser and ptx-codeinfo 4.0 MB), the rest is image heap.
Tried options:

- `-Os` (optimize for size): not available in GraalVM 23.1, only in newer releases
- `-O1`: no size change
- `strip`: saves 0.2 MB
- `-H:-IncludeMethodData`: saves 1 MB, but removes method data used for stack traces

### Memory Settings

Peak memory depends on the young generation size of the serial GC. The default lets a
5000 LOC file use 57 MB. `native-image.properties` sets `-R:MaxNewSize=8m`, which
lowers this to 35 MB with about 10 ms more run time. The value can be overridden at
run time, for example `ptx-codeinfo -Xmn16m loc Large.java`.

## Performance Targets

Measured on Linux x64 with GraalVM CE 21.0.2, see [README.md](README.md#performance)
for details.

| Metric | JAR (JVM) | Native Image | Target | Target Met? |
|--------|-----------|--------------|--------|-------------|
| Startup Time | 112 ms | 2.5 ms | < 50 ms | ✅ Yes |
| Single File (100 LOC) | 171 ms | 2.5 ms | < 100 ms | ✅ Yes |
| Large File (5000 LOC) | 209 ms | 58 ms | < 500 ms | ✅ Yes |
| Memory Usage (100 LOC) | 77 MB | 19.5 MB | < 30 MB | ✅ Yes |
| Memory Usage (5000 LOC) | 202 MB | 35.4 MB | < 30 MB | ❌ No |
| Binary Size | N/A | 22.7 MB | < 20 MB | ❌ No |

## Troubleshooting

### "GraalVM native-image is missing"

Ensure `GRAALVM_HOME` is set and `native-image` is in PATH:
```bash
echo $GRAALVM_HOME
which native-image
native-image --version
```

### Reflection Errors at Runtime

If you see `ClassNotFoundException` or `NoSuchMethodException`:
1. Re-run tracing agent with more test cases
2. Check generated `reflect-config.json` includes missing classes
3. Manually add missing entries if needed

### Build Fails with Memory Error

Increase native-image memory:
```bash
native-image -J-Xmx8g -jar target/ptx-codeinfo.jar
```

## References

- [GraalVM Community Edition Downloads](https://github.com/graalvm/graalvm-ce-builds/releases)
- [Picocli on GraalVM](https://picocli.info/picocli-on-graalvm.html)
- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Configure with Tracing Agent](https://www.graalvm.org/latest/reference-manual/native-image/guides/configure-with-tracing-agent/)
- [Picocli Native Image Demo (Maven)](https://github.com/remkop/picocli-native-image-maven-demo)

## Conclusion

**ptx-codeinfo is compatible with GraalVM Community Edition**. Picocli provides excellent native image support out of the box, while JavaParser requires reflection metadata generated by the tracing agent. The Community Edition is free, well-supported, and meets all our requirements for creating fast, standalone native binaries.
