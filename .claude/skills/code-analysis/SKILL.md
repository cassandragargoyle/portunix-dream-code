---
name: code-analysis
description: Run code analysis on the project using Portunix plugins - lines of code (code-info), static analysis (SonarLint/CodeScan) and TODO/FIXME scan, with a report.
argument-hint: "[loc|static|todo|all]"
---

# Code Analysis

Run code analysis on the project using Portunix plugins.

## Available Analyses

| Type | Command | Description |
| ---- | ------- | ----------- |
| `loc` | `portunix code-info loc` | Lines of code analysis (default) |
| `static` | `portunix code-analysis` | Static code analysis (SonarLint/CodeScan) |
| `todo` | `portunix code-analysis` | TODO/FIXME scanner (via CodeScan engine) |

## Arguments

- `/code-analysis` or `/code-analysis loc` - Lines of code analysis
- `/code-analysis static` - Static code analysis (SonarLint + CodeScan)
- `/code-analysis todo` - TODO/FIXME analysis (via CodeScan engine)
- `/code-analysis all` - Run all available analyses

## Instructions

### 1. Determine analysis type from `$ARGUMENTS`

- Empty or `loc` - Lines of code analysis
- `static` - Static code analysis
- `todo` - TODO/FIXME analysis
- `all` - Run all available analyses

### 2. Check Plugin Status

Before running any analysis, verify plugins are enabled:

```bash
portunix plugin list
```

If plugins show as "ready" but fail to execute, deploy them:

```bash
make deploy-local
```

### 3. LOC Analysis

Run lines of code analysis using the code-info plugin:

```bash
# Analyze single file
portunix code-info loc <file.java>

# Analyze multiple files (use find + xargs)
find . -name "*.java" -not -path "./target/*" -exec portunix code-info loc {} \;

# Analyze with verbose output
portunix code-info loc <file.java> --verbose
```

Generate report with:

- Total files analyzed
- Total lines of code
- Average LOC per file
- Top 10 largest files
- Files grouped by module

**Thresholds:**

| LOC | Status |
| --- | ------ |
| <200 | OK |
| 200-400 | Warning |
| >400 | Needs Review |

### 4. Static Code Analysis

Run static analysis using SonarLint or CodeScan engines:

```bash
# Initialize configuration (first time)
portunix code-analysis init --template java

# Run analysis on current directory
portunix code-analysis

# Run analysis with custom config
portunix code-analysis --config ./custom.ptxcode

# Analyze specific directory
portunix code-analysis ./src

# Validate configuration file
portunix code-analysis validate ./project.ptxcode

# List available rules
portunix code-analysis rules --engine sonarlint --language java
```

### 5. TODO Analysis

TODO/FIXME scanning uses the CodeScan engine in code-analysis plugin:

```bash
# Run analysis with CodeScan (detects TODO, FIXME, HACK, XXX by default)
portunix code-analysis ./src

# Initialize with template that includes annotation detection
portunix code-analysis init --template java
```

Configure annotations in `.ptxcode` file:

```yaml
engines:
  codescan:
    enabled: true
    annotations: [TODO, FIXME, HACK, XXX]
    severity:
      TODO: INFO
      FIXME: MAJOR
      HACK: MAJOR
      XXX: MINOR
```

Categories detected by CodeScan:

- **TODO**: Tracked tasks (severity: INFO)
- **FIXME**: Issues requiring attention (severity: MAJOR)
- **HACK**: Workarounds (severity: MAJOR)
- **XXX**: Problematic code markers (severity: MINOR)
- **NOTE**: Notes (configurable)
- **BUG**: Bug markers (configurable)

#### TODO Report Format

```markdown
### TODO Analysis

#### Summary
- **Total TODOs**: X
- **Standard format**: X
- **Needs formatting**: X (TODO:XXX or legacy)
- **FIXMEs**: X
- **HACKs**: X

#### By Author
| Initials | Count |
| -------- | ----- |
| ZS | X |

#### Items Needing Attention
- [ ] TODO:XXX placeholders need proper numbering
- [ ] Legacy TODOs need reformatting
```

### 6. Generate Report

```markdown
## Code Analysis Report

### Summary
- **Analysis type**: loc | static | todo | all
- **Total files**: X
- **Total LOC**: X
- **Average LOC/file**: X

### Top 10 Largest Files
| File | LOC | Status |
| ---- | --- | ------ |
| ... | ... | OK / Warning / Review |

### Static Analysis Issues
| File | Rule | Severity | Message |
| ---- | ---- | -------- | ------- |
| ... | ... | ... | ... |

### By Module
- **codeinfo**: X files, Y LOC
- **codeanalysis**: X files, Y LOC
```

## Plugin Reference

### code-info plugin

- **Binary**: `ptx-codeinfo`
- **Commands**: `loc`
- **MCP Tools**: `count_lines_of_code`

### code-analysis plugin

- **Binary**: `ptx-codeanalysis`
- **Commands**: `analyze` (default), `init`, `validate`, `rules`
- **Engines**: SonarLint, CodeScan (includes TODO/FIXME detection)
- **MCP Tools**: `analyze_code`, `validate_config`, `list_rules`

## Troubleshooting

If plugins fail to execute:

1. Check plugin status: `portunix plugin list`
2. Deploy plugins locally: `make deploy-local`
3. Check permissions on plugin binaries
4. Verify Java 21+ is installed for SonarLint bridge
