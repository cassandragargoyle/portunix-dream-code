# Issue #001: Migrate CLI Framework to Picocli

**Status**: Open
**Priority**: Medium
**GitHub**: #1
**Type**: Enhancement
**Created**: 2025-10-29
**Assigned**: TBD
**Estimated Effort**: 40-60 hours
**Target Version**: 1.1.0

## Description

Migrate all CLI implementations from Apache Commons CLI to Picocli framework to improve developer experience, reduce boilerplate code, and provide modern CLI features.

## Related ADR

See [ADR-001: Migrate CLI Framework from Apache Commons CLI to Picocli](../adr/001-migrate-to-picocli.md) for architectural decision and rationale.

## Scope

### Affected Modules

1. **core** - `org.cassandragargoyle.dream.api.CLI` (base class)
2. **cli** - `org.cassandragargoyle.dream.analyzer.code.CodeAnalyzeCLI`
3. **devtools** - `org.cassandragargoyle.devtools.SonarLintCli`
4. **ai** - `org.cassandragargoyle.ai.cli.MainCLI`

### Files to Modify

```
core/src/main/java/org/cassandragargoyle/dream/api/CLI.java
cli/src/main/java/org/cassandragargoyle/dream/analyzer/code/CodeAnalyzeCLI.java
devtools/src/main/java/org/cassandragargoyle/devtools/SonarLintCli.java
ai/src/main/java/org/cassandragargoyle/ai/cli/MainCLI.java
pom.xml (root)
cli/pom.xml
devtools/pom.xml
ai/pom.xml
```

## Tasks

### Phase 1: Foundation (Week 1-2)

- [ ] **Task 1.1**: Add Picocli dependency to root POM
  - Add `info.picocli:picocli:4.7.5` to `<dependencyManagement>`
  - Version property: `<picocli.version>4.7.5</picocli.version>`
  - Verify no conflicts with existing dependencies

- [ ] **Task 1.2**: Create Picocli-based CLI base class
  - Refactor `org.cassandragargoyle.dream.api.CLI`
  - Add `@Command` annotation support
  - Implement `Callable<Integer>` or `Runnable`
  - Maintain backward compatibility during transition

- [ ] **Task 1.3**: Update build configuration
  - Ensure Maven Compiler Plugin handles annotations
  - Add annotation processing configuration if needed
  - Update JAR packaging for CLI executables

- [ ] **Task 1.4**: Create migration documentation
  - Write migration guide for developers
  - Document Picocli best practices
  - Create example CLI implementation

### Phase 2: Pilot Migration (Week 3-4)

- [ ] **Task 2.1**: Migrate CodeAnalyzeCLI
  - Location: `cli/src/main/java/org/cassandragargoyle/dream/analyzer/code/CodeAnalyzeCLI.java`
  - Convert `Options` to `@Option` annotations
  - Replace manual parsing with `CommandLine`
  - Update help formatter to Picocli's built-in
  - Add unit tests for new implementation

- [ ] **Task 2.2**: Integration testing
  - Test all command-line options
  - Verify help output formatting
  - Test error handling and validation
  - Benchmark performance (should be similar or better)

- [ ] **Task 2.3**: Team training
  - Conduct workshop on Picocli patterns
  - Code review session for pilot migration
  - Document lessons learned

### Phase 3: Full Migration (Week 5-6)

- [ ] **Task 3.1**: Migrate SonarLintCli
  - Location: `devtools/src/main/java/org/cassandragargoyle/devtools/SonarLintCli.java`
  - Apply patterns from CodeAnalyzeCLI migration
  - Add any module-specific options
  - Update tests

- [ ] **Task 3.2**: Migrate MainCLI
  - Location: `ai/src/main/java/org/cassandragargoyle/ai/cli/MainCLI.java`
  - Implement subcommand structure if applicable
  - Migrate all options and parameters
  - Update tests

- [ ] **Task 3.3**: Remove Apache Commons CLI dependency
  - Remove from all module POMs
  - Verify no lingering imports
  - Clean up unused code

- [ ] **Task 3.4**: Update documentation
  - Update README.md with new CLI usage
  - Update CLAUDE.md if needed
  - Create user-facing CLI documentation
  - Update any CLI examples in docs/

### Phase 4: Validation (Week 7)

- [ ] **Task 4.1**: End-to-end testing
  - Test all CLI commands across all modules
  - Verify error messages are user-friendly
  - Test edge cases and error conditions
  - Manual testing on Linux and Windows

- [ ] **Task 4.2**: Performance validation
  - Benchmark startup time
  - Compare memory usage
  - Verify JAR size impact
  - Document any regressions

- [ ] **Task 4.3**: Release preparation
  - Write release notes
  - Update CHANGELOG.md
  - Tag version 1.1.0
  - Prepare migration guide for users

## Technical Requirements

### Dependencies

```xml
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.5</version>
</dependency>
```

### Code Pattern Example

**Before (Apache Commons CLI)**:
```java
Options options = new Options();
options.addOption("f", "file", true, "File name for analyze.");
CommandLineParser parser = new DefaultParser();
CommandLine cmd = parser.parse(options, args);
if (cmd.hasOption("f")) {
    fileName = cmd.getOptionValue("file");
}
```

**After (Picocli)**:
```java
@Command(name = "analyze", description = "Analyze code files")
public class CodeAnalyzeCLI implements Callable<Integer> {
    @Option(names = {"-f", "--file"}, description = "File name for analyze")
    private File file;

    @Override
    public Integer call() {
        // Implementation
        return 0;
    }
}
```

## Testing Strategy

1. **Unit Tests**: Test individual command classes with Picocli test utilities
2. **Integration Tests**: Test CLI execution with various argument combinations
3. **Regression Tests**: Ensure existing CLI behavior is preserved
4. **Manual Tests**: Test user experience improvements (help, colors, etc.)

## Success Criteria

- [ ] All CLI modules migrated to Picocli
- [ ] Zero regressions in existing CLI functionality
- [ ] Help output improved with colors and better formatting
- [ ] Code size reduced by at least 30% in CLI classes
- [ ] All tests passing
- [ ] Documentation updated

## Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Breaking user scripts | High | Maintain CLI argument compatibility |
| Performance regression | Medium | Benchmark during pilot phase |
| Team unfamiliarity | Low | Training and pair programming |
| Unexpected bugs | Medium | Comprehensive testing, gradual rollout |

## Notes

- Keep Apache Commons CLI as optional dependency during transition
- Consider feature flags for parallel CLI implementations
- Monitor user feedback during rollout
- Plan rollback strategy if critical issues arise

## References

- ADR: [docs/adr/001-migrate-to-picocli.md](../adr/001-migrate-to-picocli.md)
- Picocli: https://picocli.info/
- Migration Guide: https://picocli.info/#_migrating_from_commons_cli

## Comments

<!-- Add implementation notes and progress updates here -->
