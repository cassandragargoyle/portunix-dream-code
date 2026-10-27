// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package config

import (
	"fmt"
)

// Validate checks the configuration for errors
func Validate(cfg *Config) []string {
	var errors []string

	// Check required fields
	if cfg.APIVersion == "" {
		errors = append(errors, "apiVersion is required")
	} else if cfg.APIVersion != "portunix.ai/v1" {
		errors = append(errors, fmt.Sprintf("unsupported apiVersion: %s (expected: portunix.ai/v1)", cfg.APIVersion))
	}

	if cfg.Kind == "" {
		errors = append(errors, "kind is required")
	} else if cfg.Kind != "CodeAnalysis" {
		errors = append(errors, fmt.Sprintf("unsupported kind: %s (expected: CodeAnalysis)", cfg.Kind))
	}

	if cfg.Metadata.Name == "" {
		errors = append(errors, "metadata.name is required")
	}

	// Check targets
	if len(cfg.Spec.Targets.Include) == 0 {
		errors = append(errors, "spec.targets.include must have at least one pattern")
	}

	// Check engines
	if cfg.Spec.Engines.SonarLint == nil && cfg.Spec.Engines.CodeScan == nil {
		errors = append(errors, "at least one engine must be configured")
	}

	// Validate SonarLint config
	if cfg.Spec.Engines.SonarLint != nil && cfg.Spec.Engines.SonarLint.Enabled {
		if err := validateSonarLint(cfg.Spec.Engines.SonarLint); err != nil {
			errors = append(errors, err...)
		}
	}

	// Validate CodeScan config
	if cfg.Spec.Engines.CodeScan != nil && cfg.Spec.Engines.CodeScan.Enabled {
		if err := validateCodeScan(cfg.Spec.Engines.CodeScan); err != nil {
			errors = append(errors, err...)
		}
	}

	// Validate thresholds
	if cfg.Spec.Thresholds.Blocker < -1 {
		errors = append(errors, "spec.thresholds.blocker must be >= -1")
	}
	if cfg.Spec.Thresholds.Critical < -1 {
		errors = append(errors, "spec.thresholds.critical must be >= -1")
	}
	if cfg.Spec.Thresholds.Major < -1 {
		errors = append(errors, "spec.thresholds.major must be >= -1")
	}
	if cfg.Spec.Thresholds.Minor < -1 {
		errors = append(errors, "spec.thresholds.minor must be >= -1")
	}

	// Validate report formats
	for i, format := range cfg.Spec.Reports.Formats {
		if format.Type == "" {
			errors = append(errors, fmt.Sprintf("spec.reports.formats[%d].type is required", i))
		} else if !isValidReportType(format.Type) {
			errors = append(errors, fmt.Sprintf("spec.reports.formats[%d].type '%s' is not valid (allowed: console, json, sarif, html)", i, format.Type))
		}
	}

	return errors
}

func validateSonarLint(cfg *SonarLintConfig) []string {
	var errors []string

	validLanguages := map[string]bool{
		"java": true, "javascript": true, "typescript": true, "python": true,
		"go": true, "kotlin": true, "scala": true, "ruby": true, "php": true,
	}

	for _, lang := range cfg.Languages {
		if !validLanguages[lang] {
			errors = append(errors, fmt.Sprintf("unsupported SonarLint language: %s", lang))
		}
	}

	return errors
}

func validateCodeScan(cfg *CodeScanConfig) []string {
	var errors []string

	if len(cfg.Annotations) == 0 && len(cfg.Patterns) == 0 {
		errors = append(errors, "CodeScan requires at least annotations or patterns to be configured")
	}

	return errors
}

func isValidReportType(t string) bool {
	valid := map[string]bool{
		"console": true,
		"json":    true,
		"sarif":   true,
		"html":    true,
	}
	return valid[t]
}
