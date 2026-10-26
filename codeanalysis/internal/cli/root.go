package cli

import (
	"github.com/spf13/cobra"
)

var (
	cfgFile string
	ciMode  bool
)

// Execute runs the root command
func Execute(version string) error {
	rootCmd := newRootCmd(version)
	return rootCmd.Execute()
}

func newRootCmd(version string) *cobra.Command {
	rootCmd := &cobra.Command{
		Use:   "ptx-codeanalysis",
		Short: "Code analysis plugin for Portunix",
		Long: `ptx-codeanalysis integrates SonarLint and CodeScan engines
for static code analysis with YAML-based configuration.

Run 'ptx-codeanalysis init' to create a .ptxcode configuration file.`,
		Version: version,
	}

	// Global flags
	rootCmd.PersistentFlags().StringVarP(&cfgFile, "config", "c", "", "config file (default: .ptxcode in current directory)")
	rootCmd.PersistentFlags().BoolVar(&ciMode, "ci", false, "CI mode - strict exit codes based on thresholds")

	// Add subcommands
	rootCmd.AddCommand(newAnalyzeCmd())
	rootCmd.AddCommand(newInitCmd())
	rootCmd.AddCommand(newValidateCmd())
	rootCmd.AddCommand(newRulesCmd())

	return rootCmd
}
