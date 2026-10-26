# ptx-sonarlint-bridge

Java bridge for SonarLint integration with ptx-codeanalysis.

## Features

- **Static Code Analysis**: Leverages SonarLint for deep code quality analysis
- **Multi-Language Support**: Java, TypeScript, JavaScript, Python, Go, Kotlin, and more
- **Rule Management**: Enable/disable rules, configure parameters
- **JSON Protocol**: Communicates via stdin/stdout JSON for easy integration

## Requirements

- Java 17+
- Maven 3.6+

## Build

```bash
# Build JAR
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Output: target/ptx-sonarlint-bridge.jar
```

Or use the parent Makefile:

```bash
cd ../.. && make build-bridge
```

## Usage

The bridge reads JSON from stdin and writes results to stdout.

### Commands

#### analyze

Run SonarLint analysis on specified files.

```bash
java -jar target/ptx-sonarlint-bridge.jar analyze
```

#### rules

List available SonarLint rules.

```bash
java -jar target/ptx-sonarlint-bridge.jar rules
```

### Input Format (analyze)

```json
{
  "files": ["src/Main.java", "src/Utils.java"],
  "languages": ["java", "typescript"],
  "rules": {
    "disable": ["java:S106"],
    "enable": ["java:S1234"],
    "params": {
      "java:S100": "format=^[a-z][a-zA-Z0-9]*$"
    }
  }
}
```

### Output Format

```json
{
  "issues": [
    {
      "ruleKey": "java:S106",
      "message": "Replace this use of System.out by a logger.",
      "filePath": "src/Main.java",
      "startLine": 10,
      "endLine": 10,
      "startColumn": 9,
      "endColumn": 30,
      "severity": "MINOR",
      "language": "java"
    }
  ],
  "stats": {
    "filesAnalyzed": 2
  }
}
```

### Example

```bash
# Analyze Java files
echo '{"files": ["src/Main.java"], "languages": ["java"]}' | \
  java -jar target/ptx-sonarlint-bridge.jar analyze

# Analyze with disabled rules
echo '{"files": ["src/Main.java"], "languages": ["java"], "rules": {"disable": ["java:S106"]}}' | \
  java -jar target/ptx-sonarlint-bridge.jar analyze | jq .

# List available rules
java -jar target/ptx-sonarlint-bridge.jar rules
```

## Supported Languages

| Language | SonarLint Plugin |
|----------|------------------|
| Java | sonar-java-plugin |
| JavaScript | sonar-javascript-plugin |
| TypeScript | sonar-javascript-plugin |
| Python | sonar-python-plugin |
| Go | sonar-go-plugin |
| Kotlin | sonar-kotlin-plugin |
| Scala | sonar-scala-plugin |
| Ruby | sonar-ruby-plugin |
| PHP | sonar-php-plugin |

## Common Rules

### Java

| Rule | Description |
|------|-------------|
| `java:S106` | System.out/err should not be used |
| `java:S1186` | Methods should not be empty |
| `java:S1135` | Track uses of "TODO" tags |
| `java:S2259` | Null pointers should not be dereferenced |

### TypeScript/JavaScript

| Rule | Description |
|------|-------------|
| `typescript:S1481` | Unused local variables |
| `javascript:S3776` | Cognitive Complexity |
| `typescript:S4325` | Type assertion style |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PTX_SONARLINT_BRIDGE` | Override path to bridge JAR |

## Testing

```bash
mvn test
```

## Architecture

```
stdin (JSON) → AnalyzeCommand → SonarLintAnalyzer → stdout (JSON)
                    │
                    └── RulesCommand → stdout (JSON)
```

The bridge is designed to be called as a subprocess by the Go orchestrator (`ptx-codeanalysis`).

## Troubleshooting

### "Java runtime not found"

Ensure Java 17+ is installed and available in PATH:

```bash
java -version
```

### "SonarLint plugin not loaded"

Check that all required SonarLint plugins are bundled in the JAR. The shade plugin should include all dependencies.

### Memory issues

For large codebases, increase JVM heap:

```bash
java -Xmx2g -jar target/ptx-sonarlint-bridge.jar analyze
```
