# ADR-003: AI-Assisted Code Transformation for Module Independence

**Date**: 2025-10-29
**Status**: Proposed
**Deciders**: Architecture Team

## Context

The `codeinfo` module (ptx-codeinfo application) is designed as a completely standalone tool that should not have runtime dependencies on other internal project modules. However, it may need functionality from internal libraries like:
- Common utilities from `core` module
- Logging utilities from `org.cassandragargoyle.api`
- Shared data structures or algorithms

### Current Challenge

**Problem**: How to reuse internal project code without creating Maven dependencies?

**Options Considered**:
1. **Maven dependencies**: Creates tight coupling, increases binary size, complicates distribution
2. **Copy-paste**: Simple but error-prone, no tracking of changes, maintenance nightmare
3. **Git submodules/subtrees**: Complex, not suitable for selective code reuse
4. **Manual refactoring**: Time-consuming, inconsistent, requires deep understanding of code

### Requirements

1. **Independence**: codeinfo must remain dependency-free from internal modules
2. **Code Reuse**: Avoid reimplementing existing functionality
3. **Maintainability**: Track which code was copied and from where
4. **Adaptability**: Transform copied code to fit codeinfo's package structure
5. **Efficiency**: Minimize developer time and potential for errors

## Decision

We will implement an **AI-assisted code transformation system** for copying and adapting code from internal modules into `codeinfo`. This system will be developed later, after the initial codeinfo implementation (v0.1.0).

### Approach

**Phase 1: Manual Process** (v0.1.0 - Initial Release)
- Copy needed code manually
- Document source location in comments
- Simple package/import refactoring by hand
- Establish patterns and requirements

**Phase 2: AI-Assisted Transformation** (v0.2.0+)
- Develop sophisticated transformation tool using AI assistants
- Automate copying, package restructuring, and dependency resolution
- Generate transformation metadata and tracking
- Enable reproducible transformations

### AI-Assisted Transformation System

The system will leverage AI (Claude, GPT, or local models) to:

1. **Analyze Source Code**
   - Identify dependencies within copied code
   - Map package structures
   - Detect required supporting classes

2. **Transform Code**
   - Rename packages to match codeinfo structure
   - Adapt imports and references
   - Simplify code by removing unused features
   - Optimize for GraalVM Native Image compatibility

3. **Generate Metadata**
   - Track source module and file
   - Record transformation date and version
   - Document changes made
   - Create bidirectional mapping

4. **Validation**
   - Verify code compiles
   - Ensure no external dependencies introduced
   - Check GraalVM compatibility
   - Run automated tests

### Example Transformation

**Source** (from `core/org/cassandragargoyle/dream/api/CLI.java`):
```java
package org.cassandragargoyle.dream.api;

public abstract class CLI implements Runnable {
    protected String[] args;
    // ... implementation
}
```

**Target** (in `codeinfo/org/cassandragargoyle/ptx/codeinfo/util/BaseCommand.java`):
```java
package org.cassandragargoyle.ptx.codeinfo.util;

// Transformed from: core/org/cassandragargoyle/dream/api/CLI.java
// Transformation: 2025-10-29, ADR-003
// Changes: Renamed CLI -> BaseCommand, adapted for Picocli
public abstract class BaseCommand implements Callable<Integer> {
    // ... adapted implementation
}
```

## Architecture Variants

### Variant A: Claude Code Integration (Recommended)
- Use Claude Code as transformation engine
- Provide specific transformation instructions via prompts
- Interactive refinement of transformations
- Leverages existing project context

**Pros**:
- Already integrated in project workflow
- High-quality code understanding
- Interactive feedback loop
- Context-aware transformations

**Cons**:
- Requires manual invocation
- Not fully automated
- Depends on Claude Code availability

### Variant B: Custom Python/Java Tool
- Develop dedicated CLI tool for transformations
- Use OpenAI API or local LLMs
- Batch processing capabilities
- Scriptable and reproducible

**Pros**:
- Fully automated
- Consistent results
- Can be integrated into build process
- Offline capable (with local models)

**Cons**:
- Significant development effort
- Maintenance overhead
- May require API keys/costs

### Variant C: Template-Based System
- Define transformation templates
- Use AST manipulation (JavaParser)
- Rule-based transformations
- No AI required

**Pros**:
- Predictable results
- Fast execution
- No external dependencies
- Deterministic

**Cons**:
- Limited flexibility
- Complex rules for edge cases
- Doesn't handle novel patterns
- High initial setup cost

**Decision**: Start with Variant A (Claude Code) for flexibility and developer experience. Evaluate Variant B if automation becomes critical in v0.3.0+.

## Consequences

### Positive

1. **Module Independence**: codeinfo remains truly standalone
2. **Binary Optimization**: No unnecessary dependencies, smaller binary size
3. **Flexibility**: Can adapt code to specific needs during transformation
4. **Documentation**: Clear tracking of code origins and changes
5. **GraalVM Optimization**: Transform code to be native-image friendly
6. **Learning**: Transformation patterns become reusable knowledge

### Negative

1. **Code Duplication**: Same code exists in multiple places
2. **Sync Challenges**: Updates to source code don't auto-propagate
3. **Development Time**: Initial setup and transformation take time
4. **Maintenance**: Must track transformations and update when needed
5. **Complexity**: Additional tooling and processes to maintain

### Neutral

1. **AI Dependency**: Relies on AI assistants for optimal workflow
2. **Manual Oversight**: Human review still required for transformations
3. **Evolution**: Approach will evolve based on practical experience

## Technical Details

### Transformation Metadata Format

Each transformed file will include header comment:

```java
/*
 * TRANSFORMED CODE - DO NOT EDIT DIRECTLY
 *
 * Source Module: core
 * Source File: org/cassandragargoyle/dream/api/CLI.java
 * Source Version: 1.0.0.2-SNAPSHOT
 * Transformation Date: 2025-10-29
 * Transformation ADR: ADR-003
 *
 * Changes Applied:
 * - Renamed CLI -> BaseCommand
 * - Changed from Runnable to Callable<Integer>
 * - Removed unused methods: xyz()
 * - Adapted for Picocli integration
 *
 * To update: Re-run transformation from source
 */
```

### Transformation Tracking File

`codeinfo/TRANSFORMATIONS.md`:
```markdown
# Code Transformations

## core/CLI.java → util/BaseCommand.java
- Date: 2025-10-29
- Reason: Base class for commands
- Changes: Package rename, interface adaptation
- Status: Active

## core/LogFactory.java → util/Logger.java
- Date: 2025-10-29
- Reason: Simplified logging
- Changes: Removed log4j, minimal implementation
- Status: Active
```

### AI Assistant Prompt Template

```markdown
# Code Transformation Request

## Context
- Source: [module]/[path]/[file]
- Target: codeinfo/[new-path]/[new-file]
- Purpose: [why we need this code]

## Requirements
1. Rename package to org.cassandragargoyle.ptx.codeinfo.[subpackage]
2. Remove dependencies on internal libraries
3. Ensure GraalVM Native Image compatibility
4. Simplify code by removing unused features
5. Add transformation metadata header

## Constraints
- No external dependencies except Picocli, JavaParser
- Must compile with Java 21
- Follow codeinfo code style

## Output
- Transformed source code
- List of changes made
- Any concerns or issues identified
```

## Implementation Timeline

### Phase 1: Manual Process (v0.1.0) - Already Started
```
Week 1-4: Initial codeinfo development
├─ Copy needed code manually
├─ Document sources in comments
└─ Establish transformation patterns
```

### Phase 2: Claude Code Workflow (v0.2.0) - Week 8-10
```
Week 8: Workflow Design
├─ Create transformation prompt templates
├─ Document transformation procedures
└─ Create example transformations

Week 9: Testing
├─ Test transformations on real code
├─ Refine prompts and procedures
└─ Document lessons learned

Week 10: Documentation
├─ Write transformation guide
├─ Create video/tutorial
└─ Team training
```

### Phase 3: Automation (v0.3.0+) - Future
```
TBD: Automation Tool
├─ Evaluate need for full automation
├─ Design CLI tool if needed
└─ Implement batch transformations
```

## ASCII Sequence Diagram

```
Developer    Claude Code    Source Code    codeinfo      Metadata
    |             |              |             |              |
    |--identify needed code----->|             |              |
    |             |              |             |              |
    |--request transformation--->|             |              |
    |             |--analyze---->|             |              |
    |             |<--source-----|             |              |
    |             |                            |              |
    |             |--transform code----------->|              |
    |             |--generate metadata----------------------->|
    |             |                            |              |
    |<--transformed code & metadata------------|              |
    |                                          |              |
    |--review & commit-----------------------→ |              |
    |                                          |              |
```

## Deployment Considerations

### CI/CD Impact
- No immediate CI/CD changes required
- Future: Automated transformation validation
- Consider transformation verification in build pipeline

### Observability
- Track transformation frequency and patterns
- Monitor maintenance burden
- Measure time saved vs manual copying

### Rollback Strategy
- Git history provides full rollback capability
- Transformation metadata enables re-transformation
- Can revert to source-based approach if AI approach fails

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Transformations introduce bugs | Medium | High | Thorough testing, code review |
| Maintenance burden too high | Medium | Medium | Evaluate automation, minimize transformations |
| AI produces incorrect code | Low | Medium | Human review required, validation |
| Source code changes break transformed code | High | Medium | Periodic re-transformation, monitoring |
| Transformation complexity grows | Medium | Low | Keep transformations simple, document patterns |

## Success Criteria

**Phase 1 (Manual)**:
- [ ] Clear documentation of all copied code
- [ ] Transformation patterns established
- [ ] No compilation errors in transformed code
- [ ] GraalVM compatibility maintained

**Phase 2 (AI-Assisted)**:
- [ ] Repeatable transformation process documented
- [ ] Claude Code prompts validated and documented
- [ ] 80% time reduction vs manual transformation
- [ ] Zero critical bugs from transformations

**Phase 3 (Automated)**:
- [ ] CLI tool for batch transformations
- [ ] Automated validation pipeline
- [ ] Full transformation history tracked
- [ ] Integration with development workflow

## Related Work

### Similar Approaches in Industry

1. **Google's Copybara**
   - Tool for transforming code between repositories
   - Used internally at Google
   - Supports custom transformation rules

2. **Facebook's Jscodeshift**
   - AST-based code transformation
   - Used for large-scale refactoring
   - Codemods approach

3. **Bazel's aspects**
   - Code generation and transformation
   - Build-time code adaptation

**Learning**: These tools prove code transformation is viable at scale. Our AI-assisted approach adds intelligence to handle complex cases.

## References

- ADR-002: [Create ptx-codeinfo CLI Application](002-ptx-codeinfo-cli-application.md)
- [Google Copybara](https://github.com/google/copybara)
- [Jscodeshift](https://github.com/facebook/jscodeshift)
- [Claude Code Documentation](https://docs.claude.com/claude-code)
- Issue: [#003 - Implement AI-Assisted Code Transformation](../issues/003-ai-code-transformation.md)

## Future Considerations

1. **Bidirectional Sync**: If source code improves, propagate to transformed code
2. **Template Library**: Build library of common transformations
3. **Community Sharing**: Share transformation patterns with team
4. **Tool Integration**: Integrate with IDE for seamless workflow

## Supersedes

None

## Superseded By

None (current)
