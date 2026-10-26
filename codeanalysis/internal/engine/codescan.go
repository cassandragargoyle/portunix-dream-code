package engine

import (
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

// CodeScan bridge communication structures
type codeScanRequest struct {
	Files           []string            `json:"files"`
	Annotations     []string            `json:"annotations,omitempty"`
	Patterns        []codeScanPattern   `json:"patterns,omitempty"`
	SeverityMapping map[string]string   `json:"severityMapping,omitempty"`
}

type codeScanPattern struct {
	Name     string `json:"name"`
	Pattern  string `json:"pattern"`
	Severity string `json:"severity,omitempty"`
	Message  string `json:"message,omitempty"`
}

type codeScanResponse struct {
	Issues []codeScanIssue `json:"issues"`
	Stats  struct {
		FilesAnalyzed int `json:"filesAnalyzed"`
	} `json:"stats"`
	Error string `json:"error,omitempty"`
}

type codeScanIssue struct {
	RuleKey     string `json:"ruleKey"`
	Message     string `json:"message"`
	FilePath    string `json:"filePath"`
	StartLine   int    `json:"startLine"`
	EndLine     int    `json:"endLine"`
	StartColumn int    `json:"startColumn"`
	EndColumn   int    `json:"endColumn"`
	Severity    string `json:"severity"`
	Language    string `json:"language"`
	MatchedText string `json:"matchedText,omitempty"`
}

// runCodeScan executes CodeScan analysis via Java bridge
func (o *Orchestrator) runCodeScan(files []string) (*Results, error) {
	results := NewResults()

	// Find bridge JAR
	bridgePath, err := findCodeScanBridgePath()
	if err != nil {
		return nil, err
	}

	// Prepare request
	request := codeScanRequest{
		Files: files,
	}

	if o.config.Spec.Engines.CodeScan != nil {
		request.Annotations = o.config.Spec.Engines.CodeScan.Annotations

		// Convert patterns from config
		for _, p := range o.config.Spec.Engines.CodeScan.Patterns {
			request.Patterns = append(request.Patterns, codeScanPattern{
				Name:     p.Name,
				Pattern:  p.Pattern,
				Severity: p.Severity,
				Message:  p.Message,
			})
		}

		request.SeverityMapping = o.config.Spec.Engines.CodeScan.SeverityMapping
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
			return nil, fmt.Errorf("Java runtime not found. Please install Java 21+ to use CodeScan analysis")
		}
		return nil, fmt.Errorf("bridge execution failed: %w\nstderr: %s", err, stderr.String())
	}

	// Parse response
	var response codeScanResponse
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
			Engine:      "codescan",
			Language:    issue.Language,
		})
	}

	results.Stats.FilesAnalyzed = response.Stats.FilesAnalyzed

	return results, nil
}

// findCodeScanBridgePath locates the CodeScan bridge JAR
func findCodeScanBridgePath() (string, error) {
	// Check relative paths from executable
	execPath, _ := os.Executable()
	execDir := filepath.Dir(execPath)

	candidates := []string{
		filepath.Join(execDir, "bridges", "codescan", "target", "ptx-codescan-bridge.jar"),
		filepath.Join(execDir, "..", "bridges", "codescan", "target", "ptx-codescan-bridge.jar"),
		filepath.Join("bridges", "codescan", "target", "ptx-codescan-bridge.jar"),
	}

	// Check environment variable
	if envPath := os.Getenv("PTX_CODESCAN_BRIDGE"); envPath != "" {
		candidates = append([]string{envPath}, candidates...)
	}

	for _, path := range candidates {
		if _, err := os.Stat(path); err == nil {
			return path, nil
		}
	}

	return "", fmt.Errorf("CodeScan bridge not found. Build it with 'make build-bridge' or set PTX_CODESCAN_BRIDGE environment variable")
}
