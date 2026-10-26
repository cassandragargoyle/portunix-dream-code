# Architecture Decision Records (ADR)

This directory contains Architecture Decision Records for the portunix-dream-code project.

## What is an ADR?

An Architecture Decision Record (ADR) captures an important architectural decision made along with its context and consequences.

## ADR Format

Each ADR should follow this structure:

- **Context**: What is the issue that we're seeing that is motivating this decision or change?
- **Decision**: What is the change that we're actually proposing or doing?
- **Consequences**: What becomes easier or more difficult to do because of this change?

## ADR Index

| ID | Title | Module(s) | Status | Date |
|----|-------|-----------|--------|------|
| [001](001-migrate-to-picocli.md) | Migrate CLI Framework from Apache Commons CLI to Picocli | cli, devtools, ai, core | Proposed | 2025-10-29 |
| [002](002-ptx-codeinfo-cli-application.md) | Create ptx-codeinfo CLI Application with GraalVM Native Image | codeinfo | Proposed | 2025-10-29 |
| [003](003-ai-assisted-code-transformation.md) | AI-Assisted Code Transformation for Module Independence | codeinfo | Proposed | 2025-10-29 |
| [004](004-ptx-codeanalysis-plugin-architecture.md) | Code Analysis Plugin Architecture (ptx-codeanalysis) | codeanalysis | Implemented | 2026-01-19 |

## ADR Statuses

- **Proposed**: Under discussion
- **Accepted**: Approved and ready for implementation
- **Implemented**: Decision has been implemented
- **Deprecated**: No longer applicable
- **Superseded**: Replaced by another ADR

## Module Naming Convention

The **Module(s)** column indicates which first-level project directories (Maven modules) are affected by the ADR:
- Use module directory names as they appear in project root (e.g., `codeinfo`, `cli`, `core`)
- List multiple modules with comma separation for cross-cutting decisions
- Use descriptive names for project-wide decisions (e.g., `all`, `build system`)

## Creating New ADRs

1. Use sequential numbering (001, 002, 003, etc.)
2. Use descriptive filenames: `NNN-brief-description.md`
3. Follow the established format (Context, Decision, Consequences)
4. Consider deployment, observability, and rollback strategies
5. Include ASCII diagrams when helpful
6. Specify affected module(s) in the index
7. Update this index when adding new ADRs

## Related Documentation

- [Issue Tracking](../issues/README.md)
- [Contributing Guidelines](../contributing/README.md)
