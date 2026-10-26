# Issue #002: Implement ptx-codeinfo CLI Application

**Status**: Open
**Priority**: High
**GitHub**: #2
**Type**: Feature
**Created**: 2025-10-29
**Assigned**: TBD
**Target Version**: 1.0.2

## Description

Implement a new standalone CLI application named `ptx-codeinfo` that provides fast code analysis using GraalVM Native Image, Picocli, and JavaParser. The application will be developed in the `codeinfo` Maven module and compiled to a native binary for instant startup performance.

## Related ADR

See [ADR-002: Create ptx-codeinfo CLI Application with GraalVM Native Image](../adr/002-ptx-codeinfo-cli-application.md) for architectural decision and rationale.

## Objectives

1. Create fast, standalone CLI tool for code analysis
2. Achieve sub-100ms execution time for single file analysis
3. Implement accurate lines-of-code counting as first feature
4. Establish foundation for future code analysis features
5. Provide native binaries for Linux and Windows

## Scope

### Module Information

- **Module Directory**: `codeinfo`
- **Binary Name**: `ptx-codeinfo`
- **Package**: `org.cassandragargoyle.dream.codeinfo`
- **Main Class**: `PtxCodeInfoCLI`

### Initial Feature: Lines of Code (LOC)

**Command**: `ptx-codeinfo loc <file>`

**Counting Rules**:
- **Count**: Lines with executable code
- **Count**: Lines with both code and comments (e.g., `int x = 1; // comment`)
- **Skip**: Empty lines (whitespace only)
- **Skip**: Comment-only lines (single-line `//` or multi-line `/* */`)

**Example Output**:
```bash
$ ptx-codeinfo loc Example.java
5 lines of code in Example.java

$ ptx-codeinfo loc Example.java --verbose
Analyzed: Example.java
Total lines: 10
Code lines: 5
Comment lines: 3
Empty lines: 2
```

## Tasks

### Phase 1: Foundation (Week 1)

- [ ] **Task 1.1**: Create codeinfo Maven module
  - Create `codeinfo/pom.xml` with project metadata
  - Add to root POM `<modules>` section
  - Set artifact ID: `dream-code-codeinfo`
  - Configure Java 21 compatibility

- [ ] **Task 1.2**: Configure dependencies
  - Add Picocli 4.7.5 dependency
  - Add JavaParser 3.27.1 (from parent POM)
  - Add GraalVM SDK (provided scope)
  - Add JUnit 5 for testing

- [ ] **Task 1.3**: Setup GraalVM Native Image plugin
  - Add `native-maven-plugin` configuration
  - Set image name: `ptx-codeinfo`
  - Configure main class
  - Add build arguments (`--no-fallback`, etc.)

- [ ] **Task 1.4**: Create basic project structure
  - Create package: `org.cassandragargoyle.dream.codeinfo`
  - Create subpackages: `commands`, `analyzers`
  - Create main class: `PtxCodeInfoCLI.java`
  - Create README.md with project description

- [ ] **Task 1.5**: Implement main CLI entry point
  - Annotate with Picocli `@Command`
  - Configure application metadata (name, version, description)
  - Implement `Callable<Integer>` interface
  - Add version provider
  - Add help command

### Phase 2: Core Feature - LOC Counter (Week 2)

- [ ] **Task 2.1**: Create LinesOfCodeCommand
  - Location: `commands/LinesOfCodeCommand.java`
  - Annotate with `@Command(name = "loc")`
  - Add `@Parameters` for file input
  - Add `@Option` for verbose mode
  - Implement command execution logic

- [ ] **Task 2.2**: Implement JavaLocAnalyzer
  - Location: `analyzers/JavaLocAnalyzer.java`
  - Use JavaParser to parse source file
  - Build AST and traverse nodes
  - Implement line counting logic:
    - Identify code lines
    - Identify comment-only lines
    - Identify empty lines
    - Handle mixed code+comment lines

- [ ] **Task 2.3**: Handle edge cases
  - Empty files
  - Files with only comments
  - Files with syntax errors (partial parsing)
  - Mixed-content lines (code + comment)
  - Multi-line comments
  - Javadoc comments
  - Unicode and special characters

- [ ] **Task 2.4**: Error handling and validation
  - File not found
  - File not readable
  - Invalid Java syntax (graceful degradation)
  - Non-Java files (clear error message)
  - Permission errors

- [ ] **Task 2.5**: Output formatting
  - Simple mode: Just line count
  - Verbose mode: Detailed breakdown
  - Error messages with actionable guidance
  - Exit codes (0=success, 1=error, 2=invalid input)

### Phase 3: Testing (Week 3)

- [ ] **Task 3.1**: Unit tests for JavaLocAnalyzer
  - Test basic code counting
  - Test comment handling (single-line, multi-line)
  - Test empty line detection
  - Test mixed code+comment lines
  - Test edge cases
  - Target: 95% code coverage

- [ ] **Task 3.2**: Unit tests for LinesOfCodeCommand
  - Test command parsing
  - Test option handling
  - Test file validation
  - Mock analyzer for isolated testing

- [ ] **Task 3.3**: Integration tests
  - Test end-to-end execution
  - Test with real Java files
  - Test error scenarios
  - Verify output format

- [ ] **Task 3.4**: Create test fixtures
  - Sample Java files with known LOC counts
  - Files with various comment styles
  - Edge case files (empty, comment-only, etc.)
  - Location: `src/test/resources/fixtures/`

### Phase 4: GraalVM Native Image (Week 3-4)

- [ ] **Task 4.1**: Configure reflection for JavaParser
  - Create `reflect-config.json`
  - Add JavaParser reflection entries
  - Test with native image agent
  - Location: `src/main/resources/META-INF/native-image/`

- [ ] **Task 4.2**: Native image build
  - Build native image with Maven plugin
  - Verify binary is created
  - Test binary execution
  - Check binary size (target: < 20MB)

- [ ] **Task 4.3**: Build optimization
  - Optimize for size vs speed trade-offs
  - Enable compression if needed
  - Test startup time (target: < 50ms)
  - Profile memory usage (target: < 30MB)

- [ ] **Task 4.4**: Platform-specific builds
  - Build Linux x64 binary
  - Build Windows x64 binary
  - Test on target platforms
  - Document platform requirements

- [ ] **Task 4.5**: Integration tests for native binary
  - Run full test suite against native binary
  - Verify functionality matches JAR version
  - Test performance meets targets
  - Test error handling in native mode

### Phase 5: Documentation & Polish (Week 4)

- [ ] **Task 5.1**: User documentation
  - Write comprehensive README.md
  - Document installation instructions
  - Document usage examples
  - Add troubleshooting section

- [ ] **Task 5.2**: Developer documentation
  - Document build process
  - Document GraalVM setup
  - Document architecture overview
  - Add contribution guidelines

- [ ] **Task 5.3**: CLI help and messages
  - Write clear help text for all commands
  - Add usage examples in help
  - Improve error messages
  - Add color output with ANSI codes

- [ ] **Task 5.4**: Performance benchmarking
  - Benchmark startup time
  - Benchmark single file analysis
  - Benchmark large file analysis (5000+ LOC)
  - Compare native vs JAR performance
  - Document results

- [ ] **Task 5.5**: Release preparation
  - Create CHANGELOG.md
  - Tag version 0.1.0
  - Package binaries (tar.gz, zip)
  - Test installation from package

### Phase 6: CI/CD Integration (Optional for v0.1.0)

- [ ] **Task 6.1**: GitHub Actions workflow
  - Build and test on push
  - Build native images for releases
  - Multi-platform builds (Linux, Windows)

- [ ] **Task 6.2**: Release automation
  - Automatic GitHub releases
  - Upload binary artifacts
  - Generate release notes

## Technical Requirements

### Dependencies (pom.xml)

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

    <!-- GraalVM SDK (provided) -->
    <dependency>
        <groupId>org.graalvm.sdk</groupId>
        <artifactId>graal-sdk</artifactId>
        <version>23.1.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Build Configuration

```xml
<build>
    <plugins>
        <!-- Native Image Plugin -->
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <version>0.10.0</version>
            <configuration>
                <imageName>ptx-codeinfo</imageName>
                <mainClass>org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI</mainClass>
                <buildArgs>
                    <buildArg>--no-fallback</buildArg>
                    <buildArg>--report-unsupported-elements-at-runtime</buildArg>
                    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                </buildArgs>
            </configuration>
            <executions>
                <execution>
                    <id>build-native</id>
                    <goals>
                        <goal>compile-no-fork</goal>
                    </goals>
                    <phase>package</phase>
                </execution>
            </executions>
        </plugin>

        <!-- Regular JAR for testing -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>org.cassandragargoyle.dream.codeinfo.PtxCodeInfoCLI</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Code Structure

### Main CLI Class

```java
package org.cassandragargoyle.dream.codeinfo;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "ptx-codeinfo",
    version = "1.0.2",
    description = "Fast code analysis tool powered by GraalVM",
    subcommands = {
        LinesOfCodeCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class PtxCodeInfoCLI implements Callable<Integer> {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new PtxCodeInfoCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Show help when no command specified
        CommandLine.usage(this, System.out);
        return 0;
    }
}
```

### LOC Command

```java
package org.cassandragargoyle.dream.codeinfo.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import java.io.File;
import java.util.concurrent.Callable;

@Command(
    name = "loc",
    description = "Count lines of code in a Java source file"
)
public class LinesOfCodeCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Java source file to analyze"
    )
    private File sourceFile;

    @Option(
        names = {"-v", "--verbose"},
        description = "Show detailed breakdown"
    )
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        // Implementation
        return 0;
    }
}
```

## Success Criteria

- [ ] Native binary builds successfully on Linux and Windows
- [ ] Startup time < 50ms
- [ ] Single file analysis < 100ms
- [ ] Binary size < 20MB
- [ ] Memory usage < 30MB
- [ ] 95% test coverage
- [ ] All unit and integration tests passing
- [ ] Accurate LOC counting per specification
- [ ] Clear error messages and help text
- [ ] Complete user and developer documentation
- [ ] Ready for v0.1.0 release

## Performance Targets

| Metric | Target | Validation Method |
|--------|--------|-------------------|
| Startup Time | < 50ms | `time ptx-codeinfo --help` |
| Single File (100 LOC) | < 100ms | Benchmark with timer |
| Large File (5000 LOC) | < 500ms | Benchmark with timer |
| Binary Size | < 20MB | `ls -lh ptx-codeinfo` |
| Memory Usage | < 30MB | `/usr/bin/time -v` or Process Monitor |
| Build Time | < 3 minutes | Maven build log |

## Testing Strategy

1. **Unit Tests**: Test individual components (analyzer, command)
2. **Integration Tests**: Test complete command execution
3. **Native Image Tests**: Test native binary functionality
4. **Performance Tests**: Verify performance targets
5. **Edge Case Tests**: Empty files, syntax errors, special cases
6. **Platform Tests**: Test on Linux and Windows

## Risks and Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| JavaParser reflection issues | Medium | High | Use native-image-agent, thorough testing |
| Performance targets not met | Low | Medium | Profiling and optimization phase |
| GraalVM build complexity | Medium | Medium | Detailed documentation, CI automation |
| Platform-specific bugs | Medium | Medium | Multi-platform testing |
| Binary size exceeds target | Low | Low | Compression, dependency optimization |

## Dependencies

- GraalVM 23.1.0 or later (for development)
- Maven 3.8+
- Java 21

## Future Enhancements (Post v0.1.0)

1. **Cyclomatic Complexity**: `ptx-codeinfo complexity <file>`
2. **Import Analysis**: `ptx-codeinfo imports <file>`
3. **Batch Processing**: `ptx-codeinfo loc <directory> --recursive`
4. **Multiple Output Formats**: JSON, CSV, XML
5. **Multi-language Support**: C++, Python, JavaScript
6. **Git Integration**: Delta analysis between commits

## Notes

- Keep JAR distribution for development and testing
- Document GraalVM installation for developers
- Consider Docker image for CI/CD environments
- Plan for autocomplete generation (Bash, ZSH)

## References

- ADR: [docs/adr/002-ptx-codeinfo-cli-application.md](../adr/002-ptx-codeinfo-cli-application.md)
- ADR: [docs/adr/003-ai-assisted-code-transformation.md](../adr/003-ai-assisted-code-transformation.md)
- GraalVM: https://www.graalvm.org/
- Picocli: https://picocli.info/
- JavaParser: https://javaparser.org/

## Comments

<!-- Add implementation notes and progress updates here -->
