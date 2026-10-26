# Distribution and Code Validation Guide

This document describes how to validate, analyze, and distribute code in the portunix-dream-code project.

## Code Validation

### Lines of Code Analysis

Use the `codeinfo` tool to analyze lines of code in Java source files. This helps identify large files that may need refactoring and provides metrics for code review.

#### Quick Analysis (Top 20 Files)

```bash
make loc
```

Output example:

```text
264 lines of code in CodeScanAnalyzer.java
148 lines of code in SonarLintAnalyzer.java
116 lines of code in AnalyzeCommand.java
...
```

#### Full Analysis (All Files)

```bash
make loc-all
```

#### Detailed Analysis of Specific File

```bash
make loc-file FILE=path/to/YourFile.java
```

Output example:

```text
Analyzed: YourFile.java
Total lines: 150
Code lines: 120
Comment lines: 20
Empty lines: 10
```

### Creating a Summary with Claude Code

You can use Claude Code to generate a comprehensive code metrics summary:

#### Basic Summary

```bash
# Generate LOC report and pipe to Claude Code
make loc-all > /tmp/loc-report.txt
claude -p "Analyze this LOC report and provide: 1) Total LOC count, 2) Average file size, 3) Files that might need refactoring (>200 LOC), 4) Distribution by module" < /tmp/loc-report.txt
```

#### Interactive Analysis

Start Claude Code in the project directory and ask for analysis:

```bash
claude

# Then in Claude Code session:
> Run make loc-all and summarize the results. Identify files over 200 LOC that might benefit from refactoring.
```

#### Automated Report Generation

Create a script for regular code health checks:

```bash
#!/bin/bash
# scripts/code-health-report.sh

echo "# Code Health Report - $(date +%Y-%m-%d)" > report.md
echo "" >> report.md

echo "## Lines of Code Analysis" >> report.md
echo '```' >> report.md
make loc-all >> report.md
echo '```' >> report.md

# Use Claude Code to add analysis
claude -p "Read report.md and append a '## Analysis' section with insights about code complexity, large files, and refactoring suggestions. Keep it concise." --allowedTools Read,Edit
```

### Recommended Thresholds

| Metric | Good | Warning | Needs Review |
| ------ | ---- | ------- | ------------ |
| File LOC | < 200 | 200-400 | > 400 |
| Method LOC | < 30 | 30-50 | > 50 |
| Comment ratio | 10-30% | 5-10% | < 5% |

## Distribution

### Building Distribution Packages

```bash
# Build all distribution packages
make dist

# Build specific packages
make dist-codeanalysis    # Go binary + Java bridges
make dist-codeinfo        # JAR package
make dist-codeinfo-native # Native binary (requires GraalVM)
```

### Release Process

```bash
# Run tests and create distributions
make release
```

Output structure:

```text
dist/
├── ptx-codeanalysis/
│   ├── ptx-codeanalysis          # Go binary
│   ├── bridges/                   # Java bridges
│   └── defaults/                  # Default configurations
└── ptx-codeinfo/
    └── ptx-codeinfo.jar          # Or native binary
```

### Cross-Platform Builds

For codeanalysis module:

```bash
# Build for all platforms
make -C codeanalysis dist-all

# Build for specific platform
make -C codeanalysis dist-linux-amd64
make -C codeanalysis dist-linux-arm64
make -C codeanalysis dist-windows-amd64
make -C codeanalysis dist-darwin-amd64
make -C codeanalysis dist-darwin-arm64
```
