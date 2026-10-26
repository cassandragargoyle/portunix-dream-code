# Issue #003: Implement AI-Assisted Code Transformation

**Status**: Open
**Priority**: Low
**GitHub**: #3
**Type**: Enhancement
**Created**: 2025-10-29
**Assigned**: TBD
**Estimated Effort**: 40-60 hours
**Target Version**: 0.2.0
**Blocked By**: #002 (ptx-codeinfo v0.1.0 must be completed first)

## Description

Implement an AI-assisted code transformation system for copying and adapting code from internal project modules into the standalone `codeinfo` module. This system will enable efficient code reuse while maintaining complete module independence.

## Related ADR

See [ADR-003: AI-Assisted Code Transformation for Module Independence](../adr/003-ai-assisted-code-transformation.md) for architectural decision and rationale.

## Objectives

1. Establish repeatable workflow for code transformations using Claude Code
2. Create transformation prompt templates and documentation
3. Implement tracking system for transformed code
4. Reduce transformation time by 80% compared to manual copying
5. Maintain zero critical bugs from transformations
6. Enable future automation if needed

## Context

The `codeinfo` module must remain dependency-free from other internal modules to ensure:
- Minimal binary size for GraalVM Native Image
- Complete independence for standalone distribution
- No version coupling with other project modules

However, reusing internal code (utilities, common patterns) is more efficient than reimplementing. This issue addresses how to do this safely and efficiently.

## Scope

### Phase 1: Manual Process (Already Done in v0.1.0)
- Document all manually copied code
- Establish transformation patterns
- Identify common transformation needs

### Phase 2: AI-Assisted Workflow (This Issue - v0.2.0)
- Create Claude Code transformation templates
- Document transformation procedures
- Implement metadata tracking system
- Train team on workflow
- Validate approach with real transformations

### Phase 3: Future Automation (v0.3.0+, Separate Issue)
- Evaluate need for full automation
- Design CLI tool if beneficial
- Implement batch transformations

## Tasks

### Phase 1: Analysis & Planning (Week 1)

- [ ] **Task 1.1**: Analyze v0.1.0 transformations
  - Review all code copied during codeinfo v0.1.0 development
  - Document source locations and changes made
  - Identify common transformation patterns
  - Categorize complexity levels

- [ ] **Task 1.2**: Define transformation types
  - Simple: Package rename only
  - Medium: Package rename + minor API changes
  - Complex: Major restructuring, dependency removal
  - Document characteristics of each type

- [ ] **Task 1.3**: Design metadata format
  - File header format for transformation metadata
  - TRANSFORMATIONS.md format for central tracking
  - Git commit message format
  - Documentation template

### Phase 2: Claude Code Integration (Week 2-3)

- [ ] **Task 2.1**: Create prompt templates
  - Template for simple transformations
  - Template for medium transformations
  - Template for complex transformations
  - Template for validation/review
  - Store in `.claude/prompts/transformations/`

- [ ] **Task 2.2**: Document transformation workflow
  - Step-by-step procedure in docs
  - Screenshots/examples
  - Common pitfalls and solutions
  - Quality checklist

- [ ] **Task 2.3**: Create helper scripts
  - Script to generate transformation request
  - Script to validate transformed code
  - Script to update TRANSFORMATIONS.md
  - Store in `scripts/transformations/`

- [ ] **Task 2.4**: Implement metadata tracking
  - Create codeinfo/TRANSFORMATIONS.md
  - Add header comments to transformed files
  - Link to source files in comments
  - Version tracking

### Phase 3: Testing & Validation (Week 3-4)

- [ ] **Task 3.1**: Test transformations with real code
  - Select 3-5 files needing transformation
  - Apply AI-assisted workflow
  - Document time taken vs manual approach
  - Identify issues and improvements

- [ ] **Task 3.2**: Validate transformed code
  - Code compiles without errors
  - No external dependencies introduced
  - GraalVM Native Image compatibility
  - Unit tests pass
  - No functionality regressions

- [ ] **Task 3.3**: Code review process
  - Define review checklist
  - Document review workflow
  - Test with team members
  - Gather feedback

### Phase 4: Documentation & Training (Week 4)

- [ ] **Task 4.1**: Write comprehensive guide
  - Location: `docs/guides/code-transformation.md`
  - Include examples and screenshots
  - Cover common scenarios
  - Troubleshooting section

- [ ] **Task 4.2**: Create prompt library
  - Document all tested prompts
  - Include usage examples
  - Tips for different scenarios
  - Location: `.claude/prompts/transformations/`

- [ ] **Task 4.3**: Team training
  - Conduct workshop/demo
  - Hands-on practice session
  - Q&A and feedback
  - Record video tutorial (optional)

- [ ] **Task 4.4**: Update project documentation
  - Update CLAUDE.md with transformation workflow
  - Link from codeinfo README
  - Add to contributing guidelines
  - Update ADR-003 with lessons learned

### Phase 5: Metrics & Evaluation (Week 4)

- [ ] **Task 5.1**: Measure effectiveness
  - Time saved vs manual approach
  - Error rate comparison
  - Developer satisfaction survey
  - Quality metrics

- [ ] **Task 5.2**: Evaluate automation need
  - Frequency of transformations
  - Complexity distribution
  - ROI of building automation tool
  - Decision document for Phase 3

## Technical Details

### Transformation Metadata Format

**File Header**:
```java
/*
 * TRANSFORMED CODE - DO NOT EDIT DIRECTLY
 *
 * Source Module: core
 * Source File: org/cassandragargoyle/dream/api/CLI.java
 * Source Version: 1.0.0.2-SNAPSHOT
 * Transformation Date: 2025-10-29
 * Transformation ADR: ADR-003
 * Transformation Issue: #003
 *
 * Changes Applied:
 * - Renamed CLI -> BaseCommand
 * - Changed from Runnable to Callable<Integer>
 * - Removed unused methods: xyz()
 * - Adapted for Picocli integration
 *
 * To update: Re-run transformation from source
 * See: docs/guides/code-transformation.md
 */
```

**TRANSFORMATIONS.md Entry**:
```markdown
## core/CLI.java → util/BaseCommand.java
**Date**: 2025-10-29
**Type**: Medium
**Reason**: Base class for Picocli commands
**Changes**:
- Package: `org.cassandragargoyle.dream.api` → `org.cassandragargoyle.ptx.codeinfo.util`
- Class: `CLI` → `BaseCommand`
- Interface: `Runnable` → `Callable<Integer>`
- Removed: `protected String[] args` field (handled by Picocli)

**Status**: Active
**Validation**: ✓ Compiled, ✓ Tests pass, ✓ GraalVM compatible
```

### Claude Code Prompt Template Example

**Template**: `.claude/prompts/transformations/medium-transformation.md`
```markdown
# Code Transformation Request - Medium Complexity

## Source Code
- **Module**: [module-name]
- **File**: [source-path]
- **Version**: [version]

## Target
- **Module**: codeinfo
- **New Path**: [target-path]
- **New Name**: [new-class-name]

## Purpose
[Why this code is needed in codeinfo]

## Transformation Requirements

### Package Structure
- Old Package: [old-package]
- New Package: org.cassandragargoyle.ptx.codeinfo.[subpackage]

### Code Changes Needed
1. [Change 1 description]
2. [Change 2 description]
3. [Change 3 description]

### Dependencies to Remove
- [Dependency 1]: Replace with [alternative]
- [Dependency 2]: Inline or remove

### API Adaptations
- [Old API] → [New API]

## Constraints
- No external dependencies except: Picocli, JavaParser
- Must compile with Java 21
- GraalVM Native Image compatible
- Follow codeinfo code style

## Output Required
1. Transformed Java source code with metadata header
2. List of all changes made
3. TRANSFORMATIONS.md entry
4. Any concerns or edge cases identified

## Validation Checklist
- [ ] Package renamed correctly
- [ ] All imports updated
- [ ] Dependencies removed/replaced
- [ ] Metadata header added
- [ ] Code compiles
- [ ] No warnings
```

### Helper Script Example

**Script**: `scripts/transformations/prepare-transformation.sh`
```bash
#!/bin/bash
# Prepare transformation request for Claude Code

SOURCE_MODULE=$1
SOURCE_FILE=$2
TARGET_FILE=$3

echo "Preparing transformation request..."
echo "Source: $SOURCE_MODULE/$SOURCE_FILE"
echo "Target: codeinfo/$TARGET_FILE"

# Generate prompt from template
cat > /tmp/transformation-request.md <<EOF
# Code Transformation Request

Source Module: $SOURCE_MODULE
Source File: $SOURCE_FILE
Target File: codeinfo/$TARGET_FILE

[... rest of template ...]
EOF

echo "Request prepared: /tmp/transformation-request.md"
echo "Next: Open Claude Code and paste the request"
```

## Success Criteria

- [ ] Claude Code workflow documented and tested
- [ ] Prompt templates created and validated
- [ ] Metadata tracking system implemented
- [ ] 3+ successful transformations completed using workflow
- [ ] 80% time reduction vs manual copying
- [ ] Zero critical bugs from transformations
- [ ] Team trained and comfortable with workflow
- [ ] Documentation complete and accessible
- [ ] Decision made on Phase 3 automation

## Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| **Time Savings** | 80% vs manual | Track time for each transformation |
| **Error Rate** | < 5% | Bugs found in transformed code |
| **Adoption Rate** | 100% of team | Team members using workflow |
| **Transformation Quality** | 95% first-time success | Code reviews passing |
| **Documentation Quality** | 8/10 rating | Team feedback survey |

## Risks and Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| AI produces incorrect transformations | Medium | High | Mandatory code review, validation checklist |
| Workflow too complex | Low | Medium | Simplify, provide examples, training |
| Team resistance to AI workflow | Low | Low | Show benefits, make optional |
| Metadata gets out of sync | Medium | Low | Automated validation, documentation |
| Time savings not achieved | Low | Medium | Refine prompts, streamline process |

## Dependencies

**Blocked By**:
- Issue #002: ptx-codeinfo v0.1.0 must be completed first
- Need real transformation examples from v0.1.0 development

**Required**:
- Claude Code access for all team members
- Git for version control and tracking
- Time for team training and practice

## Future Enhancements (Phase 3)

1. **Automation Tool**: CLI for batch transformations
2. **Bidirectional Sync**: Update tracking when source changes
3. **Template Library**: Reusable transformation patterns
4. **CI Integration**: Validate transformations in pipeline
5. **Multi-language Support**: Extend beyond Java

## References

- ADR: [docs/adr/003-ai-assisted-code-transformation.md](../adr/003-ai-assisted-code-transformation.md)
- ADR: [docs/adr/002-ptx-codeinfo-cli-application.md](../adr/002-ptx-codeinfo-cli-application.md)
- Issue: [#002 - Implement ptx-codeinfo](002-ptx-codeinfo-implementation.md)
- [Claude Code Documentation](https://docs.claude.com/claude-code)
- [Google Copybara](https://github.com/google/copybara) (inspiration)

## Notes

This is a low-priority issue that should be started only after codeinfo v0.1.0 is completed and we have real transformation examples to learn from. The goal is to make future transformations easier, not to retrofit v0.1.0.

## Comments

<!-- Add implementation notes and progress updates here -->
