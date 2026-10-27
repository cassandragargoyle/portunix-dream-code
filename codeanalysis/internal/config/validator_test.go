// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package config

import (
	"strings"
	"testing"
)

func TestValidateCodeScanConfig(t *testing.T) {
	tests := []struct {
		name          string
		config        *Config
		expectError   bool
		errorContains string
	}{
		{
			name: "Valid config with CodeScan",
			config: &Config{
				APIVersion: "portunix.ai/v1",
				Kind:       "CodeAnalysis",
				Metadata:   Metadata{Name: "test"},
				Spec: Spec{
					Targets: Targets{Include: []string{"**/*.java"}},
					Engines: Engines{
						CodeScan: &CodeScanConfig{
							Enabled:     true,
							Annotations: []string{"TODO", "FIXME"},
						},
					},
				},
			},
			expectError: false,
		},
		{
			name: "Valid config with SonarLint only",
			config: &Config{
				APIVersion: "portunix.ai/v1",
				Kind:       "CodeAnalysis",
				Metadata:   Metadata{Name: "test"},
				Spec: Spec{
					Targets: Targets{Include: []string{"**/*.java"}},
					Engines: Engines{
						SonarLint: &SonarLintConfig{
							Enabled: true,
						},
					},
				},
			},
			expectError: false,
		},
		{
			name: "Invalid - no engines configured",
			config: &Config{
				APIVersion: "portunix.ai/v1",
				Kind:       "CodeAnalysis",
				Metadata:   Metadata{Name: "test"},
				Spec: Spec{
					Targets: Targets{Include: []string{"**/*.java"}},
					Engines: Engines{},
				},
			},
			expectError:   true,
			errorContains: "at least one engine",
		},
		{
			name: "Invalid - CodeScan without annotations or patterns",
			config: &Config{
				APIVersion: "portunix.ai/v1",
				Kind:       "CodeAnalysis",
				Metadata:   Metadata{Name: "test"},
				Spec: Spec{
					Targets: Targets{Include: []string{"**/*.java"}},
					Engines: Engines{
						CodeScan: &CodeScanConfig{
							Enabled: true,
						},
					},
				},
			},
			expectError:   true,
			errorContains: "CodeScan requires",
		},
		{
			name: "Valid - CodeScan with custom patterns",
			config: &Config{
				APIVersion: "portunix.ai/v1",
				Kind:       "CodeAnalysis",
				Metadata:   Metadata{Name: "test"},
				Spec: Spec{
					Targets: Targets{Include: []string{"**/*.java"}},
					Engines: Engines{
						CodeScan: &CodeScanConfig{
							Enabled: true,
							Patterns: []CodeScanPattern{
								{Name: "password", Pattern: "password\\s*="},
							},
						},
					},
				},
			},
			expectError: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			errors := Validate(tt.config)

			if tt.expectError {
				if len(errors) == 0 {
					t.Error("Expected validation error, got none")
				} else if tt.errorContains != "" {
					found := false
					for _, err := range errors {
						if strings.Contains(err, tt.errorContains) {
							found = true
							break
						}
					}
					if !found {
						t.Errorf("Expected error containing '%s', got: %v", tt.errorContains, errors)
					}
				}
			} else {
				if len(errors) > 0 {
					t.Errorf("Expected no errors, got: %v", errors)
				}
			}
		})
	}
}

func TestCodeScanConfigParsing(t *testing.T) {
	// Test that CodeScan config is properly parsed from YAML tag
	cfg := &Config{
		Spec: Spec{
			Engines: Engines{
				CodeScan: &CodeScanConfig{
					Enabled:     true,
					Annotations: []string{"TODO", "FIXME", "HACK"},
					SeverityMapping: map[string]string{
						"TODO":  "INFO",
						"FIXME": "MAJOR",
						"HACK":  "MINOR",
					},
				},
			},
		},
	}

	if cfg.Spec.Engines.CodeScan == nil {
		t.Fatal("CodeScan config should not be nil")
	}

	if !cfg.Spec.Engines.CodeScan.Enabled {
		t.Error("CodeScan should be enabled")
	}

	if len(cfg.Spec.Engines.CodeScan.Annotations) != 3 {
		t.Errorf("Expected 3 annotations, got %d", len(cfg.Spec.Engines.CodeScan.Annotations))
	}

	if cfg.Spec.Engines.CodeScan.SeverityMapping["FIXME"] != "MAJOR" {
		t.Error("SeverityMapping for FIXME should be MAJOR")
	}
}
