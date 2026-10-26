# ADR-001: Migrate CLI Framework from Apache Commons CLI to Picocli

**Date**: 2025-10-29
**Status**: Proposed
**Deciders**: Architecture Team

## Context

The portunix-dream-code project currently uses Apache Commons CLI for command-line argument parsing across multiple modules:

- `cli/src/main/java/org/cassandragargoyle/dream/analyzer/code/CodeAnalyzeCLI.java`
- `devtools/src/main/java/org/cassandragargoyle/devtools/SonarLintCli.java`
- `ai/src/main/java/org/cassandragargoyle/ai/cli/MainCLI.java`

### Current Challenges

1. **Boilerplate Code**: Apache Commons CLI requires significant manual code for options definition, parsing, and help generation
2. **Error Handling**: Manual validation and error message formatting needed
3. **Type Safety**: Limited type-safe argument binding
4. **Subcommands**: No native support for git-style subcommand patterns
5. **Modern Features**: Lacks autocomplete generation, color output, and interactive prompts

### Trade-off Analysis

| Aspect | Apache Commons CLI | Picocli |
|--------|-------------------|---------|
| **Complexity** | Medium (manual setup) | Low (annotation-based) |
| **Learning Curve** | Low (established) | Medium (new paradigm) |
| **Type Safety** | Manual casting | Compile-time checks |
| **Maintenance** | Higher (verbose) | Lower (declarative) |
| **Features** | Basic | Rich (autocompletion, colors, etc.) |
| **Performance** | Good | Excellent |
| **JAR Size** | ~50KB | ~400KB |
| **Community** | Stable (older) | Active (modern) |

## Decision

We will migrate from Apache Commons CLI to Picocli for all CLI implementations in the project.

### Rationale

1. **Annotation-Driven**: Reduces boilerplate by ~70% using `@Command`, `@Option`, `@Parameters`
2. **Type Safety**: Compile-time validation of argument types
3. **Subcommand Support**: Native hierarchical command structure (e.g., `dream-code analyze java`)
4. **Rich Features**:
   - ANSI color output
   - Shell autocomplete generation (Bash, ZSH, Fish)
   - Interactive prompts
   - Built-in help formatting
5. **Spring Integration**: Better compatibility with Spring Shell if needed
6. **GraalVM Native**: First-class support for native image compilation
7. **Active Development**: Regular updates and modern Java support (Java 21+)

### Implementation Variants

**Variant A: Gradual Migration** (Recommended)
- Migrate one module at a time
- Run both frameworks in parallel during transition
- Lower risk, easier rollback
- **Cost**: Longer migration timeline (~2-3 weeks)

**Variant B: Big Bang Migration**
- Migrate all modules simultaneously
- Faster completion
- **Risk**: Higher chance of breaking changes
- **Cost**: ~1 week intensive work

**Recommendation**: Variant A for production stability

## Consequences

### Positive

1. **Developer Experience**: Faster CLI development with less code
2. **User Experience**: Better error messages, colored output, autocomplete
3. **Maintainability**: Clearer code structure with annotations
4. **Future-Proof**: Modern framework with active support
5. **Testing**: Built-in test utilities for CLI applications
6. **Documentation**: Auto-generated usage docs from annotations

### Negative

1. **Migration Effort**: ~40-60 hours across all modules
2. **Dependency Size**: JAR size increase (~350KB)
3. **Learning Curve**: Team needs training on Picocli patterns
4. **Breaking Changes**: CLI interface may change during migration
5. **Third-party Risk**: Dependency on less established library (vs Apache)

### Neutral

1. **Performance**: No significant runtime impact (both perform well)
2. **Compatibility**: Java 21+ already required by project

## Deployment Considerations

### CI/CD Impact
- Update Maven dependencies in all affected modules
- Integration tests must cover new CLI behavior
- Update documentation and examples

### Observability
- Ensure logging integration works with Picocli exception handling
- Verify JFR/monitoring compatibility for CLI commands

### Rollback Strategy
- Keep Apache Commons CLI dependency during parallel run phase
- Feature flags for new CLI vs legacy CLI
- Version CLI separately from core libraries

## Migration Timeline

```
Phase 1 (Week 1-2): Foundation
├─ Add Picocli dependency to root POM
├─ Create shared CLI base classes with Picocli
└─ Update build pipeline

Phase 2 (Week 3-4): Pilot Migration
├─ Migrate CodeAnalyzeCLI (smallest module)
├─ Integration testing
└─ Team training on patterns

Phase 3 (Week 5-6): Full Migration
├─ Migrate SonarLintCli and MainCLI
├─ Remove Apache Commons CLI dependency
└─ Update documentation

Phase 4 (Week 7): Validation
├─ End-to-end testing
├─ Performance benchmarking
└─ Release notes preparation
```

## ASCII Sequence Diagram

```
User              CLI Entry     Picocli      Command Impl
  |                  |              |              |
  |--dream-code----->|              |              |
  |   analyze        |              |              |
  |                  |--parse()---->|              |
  |                  |              |--validate--->|
  |                  |              |<--OK---------|
  |                  |<--CommandLine|              |
  |                  |--execute()------------------->|
  |                  |              |              |--analyze()
  |                  |              |              |
  |<-----------result/error------------------------|
```

## References

- [Picocli Documentation](https://picocli.info/)
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/)
- [Migration Guide](https://picocli.info/#_migrating_from_commons_cli)
- Issue: [#001 - Migrate CLI to Picocli](../issues/001-migrate-cli-to-picocli.md)

## Supersedes

None

## Superseded By

None (current)
