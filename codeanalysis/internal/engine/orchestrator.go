package engine

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
)

// Orchestrator coordinates analysis engines
type Orchestrator struct {
	config *config.Config
}

// NewOrchestrator creates a new engine orchestrator
func NewOrchestrator(cfg *config.Config) *Orchestrator {
	return &Orchestrator{
		config: cfg,
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
func (o *Orchestrator) collectFiles(basePath string) ([]string, error) {
	var files []string

	info, err := os.Stat(basePath)
	if err != nil {
		return nil, err
	}

	// If it's a single file, return it
	if !info.IsDir() {
		if o.matchesPatterns(basePath) {
			return []string{basePath}, nil
		}
		return files, nil
	}

	// Walk directory
	err = filepath.Walk(basePath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Skip directories
		if info.IsDir() {
			// Check exclude patterns for directories
			if o.isExcluded(path) {
				return filepath.SkipDir
			}
			return nil
		}

		// Check if file matches include patterns and is not excluded
		if o.matchesPatterns(path) && !o.isExcluded(path) {
			files = append(files, path)
		}

		return nil
	})

	return files, err
}

// matchesPatterns checks if a file matches any include pattern
func (o *Orchestrator) matchesPatterns(path string) bool {
	for _, pattern := range o.config.Spec.Targets.Include {
		matched, err := filepath.Match(pattern, filepath.Base(path))
		if err == nil && matched {
			return true
		}
		// Try glob-style matching
		if matchGlob(pattern, path) {
			return true
		}
	}
	return false
}

// isExcluded checks if a path matches any exclude pattern
func (o *Orchestrator) isExcluded(path string) bool {
	for _, pattern := range o.config.Spec.Targets.Exclude {
		matched, err := filepath.Match(pattern, filepath.Base(path))
		if err == nil && matched {
			return true
		}
		if matchGlob(pattern, path) {
			return true
		}
	}
	return false
}

// matchGlob performs simple glob pattern matching
func matchGlob(pattern, path string) bool {
	// Handle ** patterns
	if len(pattern) > 2 && pattern[:2] == "**" {
		// Match any path component
		suffix := pattern[2:]
		if len(suffix) > 0 && suffix[0] == '/' {
			suffix = suffix[1:]
		}
		// Check if path ends with suffix pattern
		matched, _ := filepath.Match(suffix, filepath.Base(path))
		return matched
	}

	// Simple pattern matching
	matched, _ := filepath.Match(pattern, path)
	return matched
}
