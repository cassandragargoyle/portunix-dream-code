// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import (
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

// SonarLint bridge communication structures
type sonarLintRequest struct {
	Files     []string       `json:"files"`
	Languages []string       `json:"languages,omitempty"`
	Rules     sonarLintRules `json:"rules,omitempty"`
}

type sonarLintRules struct {
	Disable []string          `json:"disable,omitempty"`
	Enable  []string          `json:"enable,omitempty"`
	Params  map[string]string `json:"params,omitempty"`
}

type sonarLintResponse struct {
	Issues []sonarLintIssue `json:"issues"`
	Stats  struct {
		FilesAnalyzed int `json:"filesAnalyzed"`
	} `json:"stats"`
	Error string `json:"error,omitempty"`
}

type sonarLintIssue struct {
	RuleKey     string `json:"ruleKey"`
	Message     string `json:"message"`
	FilePath    string `json:"filePath"`
	StartLine   int    `json:"startLine"`
	EndLine     int    `json:"endLine"`
	StartColumn int    `json:"startColumn"`
	EndColumn   int    `json:"endColumn"`
	Severity    string `json:"severity"`
	Language    string `json:"language"`
}

// runSonarLint executes SonarLint analysis via Java bridge
func (o *Orchestrator) runSonarLint(files []string) (*Results, error) {
	results := NewResults()

	// Find bridge JAR
	bridgePath, err := findBridgePath("sonarlint")
	if err != nil {
		return nil, err
	}

	// Prepare request
	request := sonarLintRequest{
		Files: files,
	}

	if o.config.Spec.Engines.SonarLint != nil {
		request.Languages = o.config.Spec.Engines.SonarLint.Languages
		request.Rules = sonarLintRules{
			Disable: o.config.Spec.Engines.SonarLint.Rules.Disable,
			Enable:  o.config.Spec.Engines.SonarLint.Rules.Enable,
			Params:  o.config.Spec.Engines.SonarLint.Rules.Params,
		}
	}

	// Serialize request
	requestJSON, err := json.Marshal(request)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize request: %w", err)
	}

	// Execute bridge
	cmd := exec.Command("java", "-jar", bridgePath, "analyze")
	cmd.Stdin = bytes.NewReader(requestJSON)

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	if err := cmd.Run(); err != nil {
		// Check if Java is available
		if _, pathErr := exec.LookPath("java"); pathErr != nil {
			return nil, fmt.Errorf("Java runtime not found. Please install Java 17+ to use SonarLint analysis")
		}
		return nil, fmt.Errorf("bridge execution failed: %w\nstderr: %s", err, stderr.String())
	}

	// Parse response
	var response sonarLintResponse
	if err := json.Unmarshal(stdout.Bytes(), &response); err != nil {
		return nil, fmt.Errorf("failed to parse bridge response: %w", err)
	}

	if response.Error != "" {
		return nil, fmt.Errorf("bridge error: %s", response.Error)
	}

	// Convert issues
	for _, issue := range response.Issues {
		results.AddIssue(Issue{
			RuleID:      issue.RuleKey,
			Message:     issue.Message,
			FilePath:    issue.FilePath,
			StartLine:   issue.StartLine,
			EndLine:     issue.EndLine,
			StartColumn: issue.StartColumn,
			EndColumn:   issue.EndColumn,
			Severity:    normalizeSeverity(issue.Severity),
			Engine:      "sonarlint",
			Language:    issue.Language,
		})
	}

	results.Stats.FilesAnalyzed = response.Stats.FilesAnalyzed

	return results, nil
}

// findBridgePath locates the SonarLint bridge JAR
func findBridgePath(bridgeName string) (string, error) {
	// Check relative paths from executable
	execPath, _ := os.Executable()
	execDir := filepath.Dir(execPath)

	candidates := []string{
		filepath.Join(execDir, "bridges", bridgeName, "target", fmt.Sprintf("ptx-%s-bridge.jar", bridgeName)),
		filepath.Join(execDir, "..", "bridges", bridgeName, "target", fmt.Sprintf("ptx-%s-bridge.jar", bridgeName)),
		filepath.Join("bridges", bridgeName, "target", fmt.Sprintf("ptx-%s-bridge.jar", bridgeName)),
	}

	// Check environment variable
	if envPath := os.Getenv("PTX_SONARLINT_BRIDGE"); envPath != "" {
		candidates = append([]string{envPath}, candidates...)
	}

	for _, path := range candidates {
		if _, err := os.Stat(path); err == nil {
			return path, nil
		}
	}

	return "", fmt.Errorf("SonarLint bridge not found. Build it with 'make bridge-sonarlint' or set PTX_SONARLINT_BRIDGE environment variable")
}

// normalizeSeverity converts SonarLint severity to standard format
func normalizeSeverity(severity string) string {
	switch severity {
	case "BLOCKER":
		return "blocker"
	case "CRITICAL":
		return "critical"
	case "MAJOR":
		return "major"
	case "MINOR":
		return "minor"
	case "INFO":
		return "info"
	default:
		return "info"
	}
}
