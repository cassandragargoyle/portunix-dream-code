// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package cli

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/engine"
	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/report"
	"github.com/spf13/cobra"
)

func newAnalyzeCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "analyze [path]",
		Short: "Run code analysis",
		Long: `Run static code analysis on the specified path or current directory.

Uses .ptxcode configuration file for settings. If no config exists,
runs with default settings.`,
		Args: cobra.MaximumNArgs(1),
		RunE: runAnalyze,
	}

	return cmd
}

func runAnalyze(cmd *cobra.Command, args []string) error {
	// Determine target path
	targetPath := "."
	if len(args) > 0 {
		targetPath = args[0]
	}

	// Resolve to absolute path
	absPath, err := filepath.Abs(targetPath)
	if err != nil {
		return fmt.Errorf("invalid path: %w", err)
	}

	// Check path exists
	info, err := os.Stat(absPath)
	if err != nil {
		if os.IsNotExist(err) {
			fmt.Fprintf(os.Stderr, "Error: path does not exist: %s\n", absPath)
			os.Exit(4) // Exit code 4: Invalid target path
		}
		return fmt.Errorf("cannot access path: %w", err)
	}

	// Load configuration
	cfg, baseDir, err := loadConfig(absPath, info.IsDir())
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: configuration error: %v\n", err)
		os.Exit(2) // Exit code 2: Configuration error
	}

	// Create orchestrator
	orch := engine.NewOrchestrator(cfg, baseDir)

	// Run analysis
	results, err := orch.Run(absPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: engine error: %v\n", err)
		os.Exit(3) // Exit code 3: Engine error
	}

	// Generate report
	reporter := report.NewConsoleReporter(cfg)
	reporter.Report(results)

	// Check thresholds for CI mode
	if ciMode {
		if exceedsThresholds(results, cfg) {
			os.Exit(1) // Exit code 1: Issues exceed threshold
		}
	}

	return nil
}

// loadConfig loads the configuration and returns the base directory of target patterns
//
// The base directory is the directory of the .ptxcode file. Without a config file it is
// the target directory, or the directory of a target file
func loadConfig(path string, isDir bool) (*config.Config, string, error) {
	// Try to find config file
	var configPath string

	if cfgFile != "" {
		// Use specified config file
		configPath = cfgFile
	} else {
		// Search for .ptxcode in target directory or current directory
		searchDirs := []string{"."}
		if isDir {
			searchDirs = append([]string{path}, searchDirs...)
		} else {
			searchDirs = append([]string{filepath.Dir(path)}, searchDirs...)
		}

		for _, dir := range searchDirs {
			candidate := filepath.Join(dir, ".ptxcode")
			if _, err := os.Stat(candidate); err == nil {
				configPath = candidate
				break
			}
		}
	}

	if configPath == "" {
		// No config found, use defaults relative to the target
		baseDir := path
		if !isDir {
			baseDir = filepath.Dir(path)
		}
		return config.DefaultConfig(), baseDir, nil
	}

	baseDir, err := filepath.Abs(filepath.Dir(configPath))
	if err != nil {
		return nil, "", fmt.Errorf("invalid config path: %w", err)
	}

	cfg, err := config.Load(configPath)
	if err != nil {
		return nil, "", err
	}
	return cfg, baseDir, nil
}

func exceedsThresholds(results *engine.Results, cfg *config.Config) bool {
	counts := results.CountBySeverity()

	if cfg.Spec.Thresholds.Blocker >= 0 && counts["blocker"] > cfg.Spec.Thresholds.Blocker {
		return true
	}
	if cfg.Spec.Thresholds.Critical >= 0 && counts["critical"] > cfg.Spec.Thresholds.Critical {
		return true
	}
	if cfg.Spec.Thresholds.Major >= 0 && counts["major"] > cfg.Spec.Thresholds.Major {
		return true
	}
	if cfg.Spec.Thresholds.Minor >= 0 && counts["minor"] > cfg.Spec.Thresholds.Minor {
		return true
	}

	return false
}
