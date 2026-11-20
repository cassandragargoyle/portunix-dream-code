// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
)

// Orchestrator coordinates analysis engines
type Orchestrator struct {
	config  *config.Config
	baseDir string
}

// NewOrchestrator creates a new engine orchestrator
//
// baseDir is the directory that target patterns are relative to (the directory of the
// .ptxcode file). An empty baseDir means the analysis target directory
func NewOrchestrator(cfg *config.Config, baseDir string) *Orchestrator {
	return &Orchestrator{
		config:  cfg,
		baseDir: baseDir,
	}
}

// Run executes analysis on the given path
func (o *Orchestrator) Run(path string) (*Results, error) {
	results := NewResults()

	// Collect files to analyze
	files, err := o.collectFiles(path)
	if err != nil {
		return nil, fmt.Errorf("failed to collect files: %w", err)
	}

	results.Stats.FilesAnalyzed = len(files)

	if len(files) == 0 {
		return results, nil
	}

	// Run SonarLint if enabled
	if o.config.Spec.Engines.SonarLint != nil && o.config.Spec.Engines.SonarLint.Enabled {
		sonarResults, err := o.runSonarLint(files)
		if err != nil {
			return nil, fmt.Errorf("SonarLint analysis failed: %w", err)
		}
		results.Merge(sonarResults)
	}

	// Run CodeScan if enabled
	if o.config.Spec.Engines.CodeScan != nil && o.config.Spec.Engines.CodeScan.Enabled {
		csResults, err := o.runCodeScan(files)
		if err != nil {
			return nil, fmt.Errorf("CodeScan analysis failed: %w", err)
		}
		results.Merge(csResults)
	}

	return results, nil
}

// collectFiles finds all files matching the target patterns
//
// Patterns are matched against the path relative to the base directory (the directory
// of the .ptxcode file). A path outside the base directory is matched relative to the
// analysis target instead
func (o *Orchestrator) collectFiles(targetPath string) ([]string, error) {
	var files []string

	info, err := os.Stat(targetPath)
	if err != nil {
		return nil, err
	}

	// If it's a single file, return it
	if !info.IsDir() {
		rel := o.relativePath(targetPath, filepath.Dir(targetPath))
		if o.matchesPatterns(rel) && !o.isExcluded(rel) {
			return []string{targetPath}, nil
		}
		return files, nil
	}

	// Walk directory
	err = filepath.Walk(targetPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		rel := o.relativePath(path, targetPath)

		if info.IsDir() {
			// Skip excluded directories, but never the base directory itself
			if rel != "." && o.isDirExcluded(rel) {
				return filepath.SkipDir
			}
			return nil
		}

		// Check if file matches include patterns and is not excluded
		if o.matchesPatterns(rel) && !o.isExcluded(rel) {
			files = append(files, path)
		}

		return nil
	})

	return files, err
}

// relativePath returns the slash-separated path relative to the base directory,
// or relative to the fallback directory when the path is outside the base directory
func (o *Orchestrator) relativePath(path, fallback string) string {
	base := o.baseDir
	if base == "" {
		base = fallback
	}
	rel, err := filepath.Rel(base, path)
	if err != nil || rel == ".." || strings.HasPrefix(rel, ".."+string(filepath.Separator)) {
		rel, err = filepath.Rel(fallback, path)
		if err != nil {
			rel = filepath.Base(path)
		}
	}
	return filepath.ToSlash(rel)
}

// matchesPatterns checks if a relative file path matches any include pattern
func (o *Orchestrator) matchesPatterns(rel string) bool {
	for _, pattern := range o.config.Spec.Targets.Include {
		if matchPattern(pattern, rel) {
			return true
		}
	}
	return false
}

// isExcluded checks if a relative file path matches any exclude pattern
func (o *Orchestrator) isExcluded(rel string) bool {
	for _, pattern := range o.config.Spec.Targets.Exclude {
		if matchPattern(pattern, rel) {
			return true
		}
	}
	return false
}

// isDirExcluded checks if a relative directory path is removed by an exclude pattern
func (o *Orchestrator) isDirExcluded(rel string) bool {
	for _, pattern := range o.config.Spec.Targets.Exclude {
		if matchDirPattern(pattern, rel) {
			return true
		}
	}
	return false
}
