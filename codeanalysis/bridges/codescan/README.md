# ptx-codescan-bridge

Java-based code scanner for annotation detection and custom pattern matching.

## Features

- **Annotation Detection**: Finds TODO, FIXME, HACK, XXX, NOTE, BUG comments
- **Custom Patterns**: Define regex patterns with configurable severity
- **Multi-Language**: Analyzes Java, TypeScript, JavaScript, Python, Go, Kotlin, and more
- **JSON Protocol**: Communicates via stdin/stdout JSON for easy integration

## Requirements

- Java 21+
- Maven 3.6+

## Build

```bash
# Build JAR
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Output: target/ptx-codescan-bridge.jar
```

Or use the parent Makefile:

```bash
cd ../.. && make build-bridge
```

## Usage

The bridge reads JSON from stdin and writes results to stdout.

### Command

```bash
java -jar target/ptx-codescan-bridge.jar analyze
```

### Input Format

```json
{
  "files": ["src/Main.java", "src/Utils.java"],
  "annotations": ["TODO", "FIXME", "HACK"],
  "patterns": [
    {
      "name": "hardcoded-password",
      "pattern": "password\\s*=\\s*[\"'][^\"']+[\"']",
      "severity": "CRITICAL",
      "message": "Hardcoded password detected"
    }
  ],
  "severityMapping": {
    "TODO": "INFO",
    "FIXME": "MAJOR",
    "HACK": "MINOR"
  }
}
```

### Output Format

```json
{
  "issues": [
    {
      "ruleKey": "codescan:TODO",
      "message": "TODO comment found: implement this",
      "filePath": "src/Main.java",
      "startLine": 10,
      "endLine": 10,
      "startColumn": 5,
      "endColumn": 35,
      "severity": "INFO",
      "language": "java",
      "matchedText": "TODO: implement this"
    }
  ],
  "stats": {
    "filesAnalyzed": 2
  }
}
```

### Example

```bash
# Analyze a file
echo '{"files": ["src/Main.java"], "annotations": ["TODO", "FIXME"]}' | \
  java -jar target/ptx-codescan-bridge.jar analyze

# Pretty print with jq
echo '{"files": ["src/Main.java"], "annotations": ["TODO"]}' | \
  java -jar target/ptx-codescan-bridge.jar analyze | jq .
```

## Supported Languages

Detection is based on file extension:

| Extension | Language |
|-----------|----------|
| `.java` | Java |
| `.ts`, `.tsx` | TypeScript |
| `.js`, `.jsx` | JavaScript |
| `.py` | Python |
| `.go` | Go |
| `.kt` | Kotlin |
| `.rb` | Ruby |
| `.php` | PHP |
| `.cs` | C# |
| `.cpp`, `.cc`, `.cxx` | C++ |
| `.c`, `.h` | C |
| `.rs` | Rust |
| `.swift` | Swift |
| `.scala` | Scala |

## Default Severities

| Annotation | Default Severity |
|------------|------------------|
| TODO | INFO |
| FIXME | MAJOR |
| HACK | MAJOR |
| XXX | MINOR |
| NOTE | INFO |
| BUG | CRITICAL |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PTX_CODESCAN_BRIDGE` | Override path to bridge JAR |

## Testing

```bash
mvn test
```

## Architecture

```
stdin (JSON) → AnalyzeCommand → CodeScanAnalyzer → stdout (JSON)
```

The bridge is designed to be called as a subprocess by the Go orchestrator (`ptx-codeanalysis`).
