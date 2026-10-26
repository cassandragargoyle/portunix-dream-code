# ADR-002: Create ptx-codeinfo CLI Application with GraalVM Native Image

**Date**: 2025-10-29
**Status**: Proposed
**Deciders**: Architecture Team

## Context

The portunix-dream-code project needs a fast, standalone CLI tool for analyzing source code files and providing quick metrics. Current tools either:
- Require full JVM startup (slow for single-file analysis)
- Lack accurate Java parsing capabilities
- Don't integrate well with our existing codebase

### Requirements

1. **Fast execution**: Sub-second startup time for single file analysis
2. **Accurate parsing**: Proper handling of Java syntax, comments, and whitespace
3. **Standalone distribution**: Single native executable without JVM dependency
4. **Extensible**: Foundation for additional code analysis features
5. **Integration**: Reuse existing javaparser infrastructure from the project

### Use Cases

- **CI/CD pipelines**: Quick code metrics without JVM overhead
- **Git hooks**: Pre-commit checks for code quality
- **Developer tools**: Instant feedback on code statistics
- **Build scripts**: Code analysis without full Java toolchain

## Decision

We will create a new CLI application named **ptx-codeinfo** with the following technology stack:

### Core Technologies

1. **GraalVM Native Image**
   - Native binary compilation for instant startup
   - Memory-efficient execution
   - Platform-specific distributions (Linux, Windows)

2. **Picocli** (aligned with ADR-001)
   - Consistent CLI framework across project
   - Native image compatibility
   - Rich CLI features (colors, autocomplete)

3. **com.github.javaparser** (already in project)
   - Accurate Java source code parsing
   - AST-based analysis
   - Comment and whitespace handling

### Initial Feature: Lines of Code (LOC)

**Specification**:
```
ptx-codeinfo loc <file>
```

**Counting Rules**:
- ✓ Count: Lines with executable code
- ✓ Count: Lines with both code and comments (e.g., `int x = 1; // comment`)
- ✗ Skip: Empty lines (whitespace only)
- ✗ Skip: Comment-only lines (single-line `//` or multi-line `/* */`)

**Example**:
```java
package com.example;              // 1: code

/**                               // skip: comment
 * Class documentation            // skip: comment
 */                               // skip: comment
public class Example {            // 2: code
                                  // skip: empty
    private int x;                // 3: code
    // This is a comment          // skip: comment
    public void test() { x++; }   // 4: code
}                                 // 5: code
```
Result: **5 lines of code**

### Architecture Variants

**Variant A: Standalone Application** (Recommended)
- Separate Maven module: `codeinfo`
- Independent versioning and releases
- Single-purpose binary (named `ptx-codeinfo`)
- **Code Independence**: Does NOT share internal project libraries via dependencies
  - Code from internal modules will be copied and transformed into codeinfo's structure
  - This ensures complete independence and minimal binary size
  - AI-assisted code transformation will be implemented later (see ADR-003)
- **Pros**: Clean separation, easy distribution, no dependency coupling
- **Cons**: Additional module to maintain, code duplication

**Variant B: Integrated Module**
- Part of existing `cli` module
- Shared dependencies
- **Pros**: Less setup overhead
- **Cons**: Coupled with other CLI tools

**Decision**: Variant A for better modularity and independent lifecycle

### GraalVM Rationale

| Aspect | Traditional JVM | GraalVM Native |
|--------|----------------|----------------|
| **Startup Time** | 500-1000ms | 10-50ms |
| **Memory Usage** | 50-100MB | 10-30MB |
| **Distribution Size** | JRE + JAR (~100MB) | Single binary (~15MB) |
| **Platform Dependency** | JVM required | Native binary |
| **Build Time** | Fast (1-5s) | Slow (1-3min) |
| **Debugging** | Full support | Limited |

For a CLI tool executing frequent short-lived commands, instant startup is critical.

### JavaParser Compatibility with GraalVM Native Image

**Overview**: JavaParser is a pure Java library that parses Java source files into an Abstract Syntax Tree (AST). It does not use JNI but does utilize reflection and ServiceLoader, particularly when working with symbol solvers or extensibility features.

**Compatibility Assessment**:

✅ **Fully Compatible** (works without configuration):
- `JavaParser.parse()` - parsing source code into AST
- Working with `CompilationUnit`, `ClassOrInterfaceDeclaration`, etc.
- Visitors: `VoidVisitorAdapter`, `GenericVisitorAdapter`
- Comment and node traversal
- Basic AST manipulation

⚠️ **Partially Compatible** (requires reflection configuration):
- `SymbolResolver`, `JavaSymbolSolver`, `CombinedTypeSolver`
- `ReflectionTypeSolver` (uses reflection)
- `TypeSolver` implementations using ClassLoader
- ServiceLoader-based extensibility

🚫 **Not Suitable for Native Build**:
- Dynamic class loading by name (`Class.forName`)
- Dynamic plugins and custom classloaders
- Runtime code generation

**For ptx-codeinfo**: We will use only the fully compatible features (basic parsing, AST traversal, visitors) for the LOC counter. Symbol resolution is not required for line counting, avoiding reflection configuration complexity.

**Mitigation Strategy**:
1. Limit usage to core JavaParser features (parsing, AST, visitors)
2. Avoid `SymbolResolver` and advanced type analysis initially
3. Create reflection configuration if future features require symbol resolution
4. Test thoroughly with native-image-agent during development

### Real-World Validation

**Proven Approach**: Several CLI applications successfully use GraalVM + JavaParser in production:

1. **quarkus-tools/formatter-cli**
   - Uses JavaParser for code formatting
   - Runs as GraalVM native image
   - Production-ready and actively maintained
   - Repository: https://github.com/quarkusio/quarkus/tree/main/devtools/cli

2. **revapi**
   - Analyzes API changes in Java code
   - Uses JavaParser for AST analysis
   - Successfully compiled to native image
   - Repository: https://github.com/revapi/revapi

**Conclusion**: GraalVM + JavaParser is a proven, reliable combination when properly configured. The key is limiting usage to non-reflective features or providing complete reflection configuration.

## Consequences

### Positive

1. **Performance**: Sub-100ms execution for single file analysis
2. **User Experience**: No JVM installation required
3. **Distribution**: Simple single-file deployment
4. **Resource Efficiency**: Minimal memory footprint
5. **CI/CD Integration**: Fast pipeline execution
6. **Reusability**: Foundation for additional code analysis tools
7. **Technology Alignment**: Uses Picocli per ADR-001

### Negative

1. **Build Complexity**: GraalVM Native Image setup required
2. **Build Time**: 1-3 minutes vs seconds for regular JAR
3. **Platform-Specific Builds**: Separate binaries for Linux/Windows/macOS
4. **Debugging Limitations**: Limited runtime reflection and debugging
5. **Initial Development**: ~2-3 weeks for first release
6. **CI/CD Resources**: Higher build machine requirements
7. **Library Constraints**: Not all Java libraries work with Native Image

### Neutral

1. **Learning Curve**: Team needs GraalVM Native Image experience
2. **Testing**: Requires testing of native binaries, not just JAR
3. **Maintenance**: Separate release pipeline for native builds

## Deployment Considerations

### Build Pipeline

```
Development Build:
┌─────────────┐
│ Maven Build │──> JAR (for testing)
└─────────────┘
       │
       v
┌─────────────┐
│ JUnit Tests │
└─────────────┘

Production Build:
┌─────────────┐
│ Maven Build │──> JAR
└─────────────┘
       │
       v
┌──────────────────┐
│ GraalVM Native   │──> Linux Binary
│ Image Compile    │──> Windows Binary
└──────────────────┘
       │
       v
┌─────────────┐
│ Integration │
│ Tests       │
└─────────────┘
       │
       v
┌─────────────┐
│ Package &   │──> tar.gz / zip
│ Release     │
└─────────────┘
```

### Distribution Strategy

1. **GitHub Releases**: Binary downloads per platform
2. **Package Managers**: Future support for apt, brew, chocolatey
3. **Container Images**: Optional Docker image with tool

### Observability

- **Logging**: Structured output for integration with logging systems
- **Exit Codes**: Standard Unix exit codes (0=success, 1=error)
- **Performance Metrics**: Optional `--timing` flag for benchmarking
- **Error Reporting**: Clear error messages with actionable guidance

### Rollback Strategy

- Keep JAR distribution alongside native binary
- Fallback to JVM execution if native binary fails
- Version compatibility with existing tools

## Technical Details

### Module Structure

**Module Directory**: `codeinfo` (application binary name: `ptx-codeinfo`)

```
codeinfo/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/cassandragargoyle/ptx/codeinfo/
│   │   │       ├── PtxCodeInfoCLI.java
│   │   │       ├── commands/
│   │   │       │   └── LinesOfCodeCommand.java
│   │   │       └── analyzers/
│   │   │           └── JavaLocAnalyzer.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── native-image/
│   │               └── reflect-config.json
│   └── test/
│       └── java/
│           └── org/cassandragargoyle/ptx/codeinfo/
│               └── LinesOfCodeCommandTest.java
└── README.md
```

### Dependencies

```xml
<dependencies>
    <!-- CLI Framework -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>4.7.5</version>
    </dependency>

    <!-- Java Parser -->
    <dependency>
        <groupId>com.github.javaparser</groupId>
        <artifactId>javaparser-core</artifactId>
        <version>3.27.1</version>
    </dependency>

    <!-- GraalVM SDK -->
    <dependency>
        <groupId>org.graalvm.sdk</groupId>
        <artifactId>graal-sdk</artifactId>
        <version>23.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Maven Plugin Configuration

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.0</version>
    <configuration>
        <imageName>ptx-codeinfo</imageName>
        <mainClass>org.cassandragargoyle.ptx.codeinfo.PtxCodeInfoCLI</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>--report-unsupported-elements-at-runtime</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

## ASCII Sequence Diagram

```
User          ptx-codeinfo    Picocli      JavaParser    FileSystem
 |                 |              |              |            |
 |--loc file.java->|              |              |            |
 |                 |--parse CLI-->|              |            |
 |                 |<--command----|              |            |
 |                 |--read()------------------------->        |
 |                 |<--file content---------------------------|
 |                 |--parse()------------->|                  |
 |                 |              |        |--build AST       |
 |                 |              |        |                  |
 |                 |<--AST-----------------|                  |
 |                 |--analyze()   |        |                  |
 |                 |  count LOC   |        |                  |
 |                 |  skip empty  |        |                  |
 |                 |  skip comments        |                  |
 |<--result: 42 LOC|              |        |                  |
 |                 |              |        |                  |
```

## Implementation Timeline

```
Phase 1 (Week 1): Foundation
├─ Create codeinfo Maven module (produces ptx-codeinfo binary)
├─ Setup GraalVM Native Image plugin
├─ Configure Picocli integration
└─ Basic project structure

Phase 2 (Week 2): Core Feature
├─ Implement LinesOfCodeCommand
├─ JavaParser integration for LOC counting
├─ Handle all comment types and edge cases
└─ Unit tests (95% coverage target)

Phase 3 (Week 3): Native Image
├─ GraalVM Native Image configuration
├─ Reflection configuration for JavaParser
├─ Build optimization for size/speed
└─ Integration tests with native binary

Phase 4 (Week 4): Polish & Release
├─ Documentation (README, man page)
├─ Error handling and user messages
├─ Performance benchmarking
└─ Release v0.1.0
```

## Future Extensions

### Planned Features

1. **Cyclomatic Complexity**: `ptx-codeinfo complexity <file>`
2. **Dependency Analysis**: `ptx-codeinfo imports <file>`
3. **Code Metrics**: `ptx-codeinfo metrics <file>` (LOC, methods, classes)
4. **Batch Processing**: `ptx-codeinfo loc <directory> --recursive`
5. **Output Formats**: JSON, CSV, XML for tool integration

### Long-term Vision

- Multi-language support (C++, Python, JavaScript)
- Integration with IDE plugins
- Git integration for delta analysis
- Code quality scoring

## Performance Targets

| Metric | Target | Justification |
|--------|--------|---------------|
| **Startup Time** | < 50ms | 20x faster than JVM |
| **Single File** | < 100ms | Instant feedback |
| **Large File (5000 LOC)** | < 500ms | Acceptable for large files |
| **Binary Size** | < 20MB | Reasonable for distribution |
| **Memory Usage** | < 30MB | Resource efficient |

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| GraalVM compatibility issues | Medium | High | Thorough testing, fallback JAR |
| JavaParser reflection issues | Medium | Medium | Reflection config, AOT testing |
| Build pipeline complexity | Low | Medium | Clear documentation, CI automation |
| Platform-specific bugs | Medium | Medium | Multi-platform testing |
| Performance not meeting targets | Low | High | Profiling, optimization phase |

## References

- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Picocli Native Image Guide](https://picocli.info/#_graalvm_native_image)
- [JavaParser Documentation](https://javaparser.org/)
- [Native Image Build Tools](https://graalvm.github.io/native-build-tools/)
- ADR-001: [Migrate CLI Framework to Picocli](001-migrate-to-picocli.md)
- Issue: [#002 - Implement ptx-codeinfo CLI Application](../issues/done/002-ptx-codeinfo-implementation.md)

## Supersedes

None

## Superseded By

None (current)
