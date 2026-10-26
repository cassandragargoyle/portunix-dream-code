package config

import "fmt"

// GetTemplate returns a configuration template by name
func GetTemplate(name string) (string, error) {
	templates := map[string]string{
		"default":    templateDefault,
		"java":       templateJava,
		"typescript": templateTypeScript,
		"python":     templatePython,
	}

	if t, ok := templates[name]; ok {
		return t, nil
	}
	return "", fmt.Errorf("unknown template: %s", name)
}

const templateDefault = `apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "project-analysis"
  description: "Code quality analysis"

spec:
  targets:
    include:
      - "src/**/*"
      - "lib/**/*"
    exclude:
      - "**/node_modules/**"
      - "**/build/**"
      - "**/target/**"
      - "**/dist/**"

  engines:
    sonarlint:
      enabled: true

  thresholds:
    blocker: 0
    critical: 5
    major: 50

  reports:
    formats:
      - type: console
`

const templateJava = `apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "java-project"
  description: "Java code quality analysis"

spec:
  targets:
    include:
      - "src/main/java/**/*.java"
      - "src/test/java/**/*.java"
    exclude:
      - "**/target/**"
      - "**/generated/**"

  engines:
    sonarlint:
      enabled: true
      languages: [java]
      rules:
        disable:
          - "java:S106"  # System.out usage

  thresholds:
    blocker: 0
    critical: 0
    major: 20

  reports:
    formats:
      - type: console
      - type: sarif
        output: ./analysis.sarif
`

const templateTypeScript = `apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "typescript-project"
  description: "TypeScript/JavaScript code quality analysis"

spec:
  targets:
    include:
      - "src/**/*.ts"
      - "src/**/*.tsx"
      - "src/**/*.js"
      - "src/**/*.jsx"
    exclude:
      - "**/node_modules/**"
      - "**/dist/**"
      - "**/build/**"
      - "**/*.test.ts"
      - "**/*.spec.ts"

  engines:
    sonarlint:
      enabled: true
      languages: [typescript, javascript]

  thresholds:
    blocker: 0
    critical: 5
    major: 30

  reports:
    formats:
      - type: console
`

const templatePython = `apiVersion: portunix.ai/v1
kind: CodeAnalysis
metadata:
  name: "python-project"
  description: "Python code quality analysis"

spec:
  targets:
    include:
      - "src/**/*.py"
      - "lib/**/*.py"
    exclude:
      - "**/__pycache__/**"
      - "**/venv/**"
      - "**/.venv/**"
      - "**/test_*.py"

  engines:
    sonarlint:
      enabled: true
      languages: [python]

    codescan:
      enabled: true
      annotations: [TODO, FIXME, HACK, XXX]

  thresholds:
    blocker: 0
    critical: 5
    major: 25

  reports:
    formats:
      - type: console
`
