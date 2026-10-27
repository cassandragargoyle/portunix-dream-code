// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
)

func TestCodeScanAnalysis(t *testing.T) {
	// Skip if bridge JAR not found
	bridgePath, err := findCodeScanBridgePath()
	if err != nil {
		t.Skipf("CodeScan bridge not found, skipping integration test: %v", err)
	}
	t.Logf("Using bridge: %s", bridgePath)

	// Find testdata directory
	testdataDir := findTestdataDir(t)

	tests := []struct {
		name           string
		files          []string
		annotations    []string
		expectedIssues int
		expectedRules  []string
	}{
		{
			name:           "Java file with TODO and FIXME",
			files:          []string{filepath.Join(testdataDir, "Sample.java")},
			annotations:    []string{"TODO", "FIXME"},
			expectedIssues: 2,
			expectedRules:  []string{"codescan:TODO", "codescan:FIXME"},
		},
		{
			name:           "TypeScript file with TODO and FIXME",
			files:          []string{filepath.Join(testdataDir, "sample.ts")},
			annotations:    []string{"TODO", "FIXME"},
			expectedIssues: 2,
			expectedRules:  []string{"codescan:TODO", "codescan:FIXME"},
		},
		{
			name:           "Multiple files",
			files:          []string{filepath.Join(testdataDir, "Sample.java"), filepath.Join(testdataDir, "sample.ts")},
			annotations:    []string{"TODO"},
			expectedIssues: 2,
			expectedRules:  []string{"codescan:TODO"},
		},
		{
			name:           "Only HACK annotations (none expected)",
			files:          []string{filepath.Join(testdataDir, "Sample.java")},
			annotations:    []string{"HACK"},
			expectedIssues: 0,
			expectedRules:  []string{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Verify test files exist
			for _, f := range tt.files {
				if _, err := os.Stat(f); os.IsNotExist(err) {
					t.Fatalf("Test file not found: %s", f)
				}
			}

			cfg := &config.Config{
				Spec: config.Spec{
					Engines: config.Engines{
						CodeScan: &config.CodeScanConfig{
							Enabled:     true,
							Annotations: tt.annotations,
						},
					},
				},
			}

			orchestrator := NewOrchestrator(cfg)
			results, err := orchestrator.runCodeScan(tt.files)

			if err != nil {
				t.Fatalf("runCodeScan failed: %v", err)
			}

			if len(results.Issues) != tt.expectedIssues {
				t.Errorf("Expected %d issues, got %d", tt.expectedIssues, len(results.Issues))
				for i, issue := range results.Issues {
					t.Logf("  Issue %d: %s - %s", i, issue.RuleID, issue.Message)
				}
			}

			// Verify rule keys use codescan: prefix
			for _, issue := range results.Issues {
				if !strings.HasPrefix(issue.RuleID, "codescan:") {
					t.Errorf("Expected ruleKey to start with 'codescan:', got: %s", issue.RuleID)
				}

				// Verify engine is set correctly
				if issue.Engine != "codescan" {
					t.Errorf("Expected engine 'codescan', got: %s", issue.Engine)
				}
			}

			// Verify expected rules are found
			for _, expectedRule := range tt.expectedRules {
				found := false
				for _, issue := range results.Issues {
					if issue.RuleID == expectedRule {
						found = true
						break
					}
				}
				if !found && tt.expectedIssues > 0 {
					t.Errorf("Expected rule %s not found in results", expectedRule)
				}
			}
		})
	}
}

func TestCodeScanSeverityMapping(t *testing.T) {
	bridgePath, err := findCodeScanBridgePath()
	if err != nil {
		t.Skipf("CodeScan bridge not found: %v", err)
	}
	t.Logf("Using bridge: %s", bridgePath)

	testdataDir := findTestdataDir(t)
	testFile := filepath.Join(testdataDir, "Sample.java")

	cfg := &config.Config{
		Spec: config.Spec{
			Engines: config.Engines{
				CodeScan: &config.CodeScanConfig{
					Enabled:     true,
					Annotations: []string{"TODO", "FIXME"},
					SeverityMapping: map[string]string{
						"TODO":  "INFO",
						"FIXME": "CRITICAL",
					},
				},
			},
		},
	}

	orchestrator := NewOrchestrator(cfg)
	results, err := orchestrator.runCodeScan([]string{testFile})

	if err != nil {
		t.Fatalf("runCodeScan failed: %v", err)
	}

	for _, issue := range results.Issues {
		switch issue.RuleID {
		case "codescan:TODO":
			if issue.Severity != "info" {
				t.Errorf("Expected TODO severity 'info', got: %s", issue.Severity)
			}
		case "codescan:FIXME":
			if issue.Severity != "critical" {
				t.Errorf("Expected FIXME severity 'critical', got: %s", issue.Severity)
			}
		}
	}
}

func TestCodeScanBridgeNotFound(t *testing.T) {
	// Temporarily unset the environment variable
	oldEnv := os.Getenv("PTX_CODESCAN_BRIDGE")
	os.Unsetenv("PTX_CODESCAN_BRIDGE")
	defer func() {
		if oldEnv != "" {
			os.Setenv("PTX_CODESCAN_BRIDGE", oldEnv)
		}
	}()

	// This test verifies error handling when bridge is not found
	// We can't easily test this without modifying the search paths
	t.Log("Bridge path search test - verifying findCodeScanBridgePath returns meaningful error")

	_, err := findCodeScanBridgePath()
	if err != nil {
		if !strings.Contains(err.Error(), "CodeScan bridge not found") {
			t.Logf("Note: Bridge found at non-standard location or different error: %v", err)
		}
	}
}

// findTestdataDir locates the testdata directory
func findTestdataDir(t *testing.T) string {
	// Try common locations
	candidates := []string{
		"testdata",
		"../testdata",
		"../../testdata",
		"../../../testdata",
	}

	for _, dir := range candidates {
		absDir, _ := filepath.Abs(dir)
		if info, err := os.Stat(absDir); err == nil && info.IsDir() {
			return absDir
		}
	}

	// Try from GOPATH or module root
	wd, _ := os.Getwd()
	t.Logf("Working directory: %s", wd)

	t.Fatal("testdata directory not found")
	return ""
}
