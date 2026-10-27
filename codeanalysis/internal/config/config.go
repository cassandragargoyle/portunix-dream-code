// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

// Config represents the .ptxcode configuration
type Config struct {
	APIVersion string   `yaml:"apiVersion"`
	Kind       string   `yaml:"kind"`
	Metadata   Metadata `yaml:"metadata"`
	Spec       Spec     `yaml:"spec"`
}

// Metadata contains project metadata
type Metadata struct {
	Name        string `yaml:"name"`
	Description string `yaml:"description,omitempty"`
}

// Spec contains the analysis specification
type Spec struct {
	Targets    Targets    `yaml:"targets"`
	Engines    Engines    `yaml:"engines"`
	Thresholds Thresholds `yaml:"thresholds,omitempty"`
	Reports    Reports    `yaml:"reports,omitempty"`
}

// Targets defines which files to analyze
type Targets struct {
	Include []string `yaml:"include"`
	Exclude []string `yaml:"exclude,omitempty"`
}

// Engines configures analysis engines
type Engines struct {
	SonarLint *SonarLintConfig `yaml:"sonarlint,omitempty"`
	CodeScan  *CodeScanConfig  `yaml:"codescan,omitempty"`
}

// SonarLintConfig configures SonarLint engine
type SonarLintConfig struct {
	Enabled   bool           `yaml:"enabled"`
	Languages []string       `yaml:"languages,omitempty"`
	Rules     SonarLintRules `yaml:"rules,omitempty"`
}

// SonarLintRules configures SonarLint rules
type SonarLintRules struct {
	Disable []string          `yaml:"disable,omitempty"`
	Enable  []string          `yaml:"enable,omitempty"`
	Params  map[string]string `yaml:"params,omitempty"`
}

// CodeScanConfig configures CodeScan engine
type CodeScanConfig struct {
	Enabled         bool                `yaml:"enabled"`
	Annotations     []string            `yaml:"annotations,omitempty"`
	Patterns        []CodeScanPattern   `yaml:"patterns,omitempty"`
	SeverityMapping map[string]string   `yaml:"severityMapping,omitempty"`
}

// CodeScanPattern defines a custom pattern for CodeScan analysis
type CodeScanPattern struct {
	Name     string `yaml:"name"`
	Pattern  string `yaml:"pattern"`
	Severity string `yaml:"severity,omitempty"`
	Message  string `yaml:"message,omitempty"`
}

// Thresholds defines CI/CD thresholds
type Thresholds struct {
	Blocker  int `yaml:"blocker"`
	Critical int `yaml:"critical"`
	Major    int `yaml:"major"`
	Minor    int `yaml:"minor"`
}

// Reports configures output reports
type Reports struct {
	Formats []ReportFormat `yaml:"formats,omitempty"`
}

// ReportFormat configures a single report format
type ReportFormat struct {
	Type   string `yaml:"type"`
	Output string `yaml:"output,omitempty"`
}

// Load reads and parses a .ptxcode file
func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("failed to read config file: %w", err)
	}

	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("failed to parse config: %w", err)
	}

	return &cfg, nil
}

// DefaultConfig returns default configuration
func DefaultConfig() *Config {
	return &Config{
		APIVersion: "portunix.ai/v1",
		Kind:       "CodeAnalysis",
		Metadata: Metadata{
			Name: "default-analysis",
		},
		Spec: Spec{
			Targets: Targets{
				Include: []string{"**/*.java", "**/*.ts", "**/*.js", "**/*.py"},
				Exclude: []string{"**/node_modules/**", "**/build/**", "**/target/**"},
			},
			Engines: Engines{
				SonarLint: &SonarLintConfig{
					Enabled: true,
				},
			},
			Thresholds: Thresholds{
				Blocker:  0,
				Critical: -1, // -1 means no limit
				Major:    -1,
				Minor:    -1,
			},
		},
	}
}
