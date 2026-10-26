package cli

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
	"github.com/spf13/cobra"
)

var initTemplate string

func newInitCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "init",
		Short: "Initialize a new .ptxcode configuration",
		Long: `Create a new .ptxcode configuration file in the current directory.

Available templates:
  default  - Basic configuration for mixed projects
  java     - Java project with SonarLint
  typescript - TypeScript/JavaScript project
  python   - Python project`,
		RunE: runInit,
	}

	cmd.Flags().StringVarP(&initTemplate, "template", "t", "default", "configuration template")

	return cmd
}

func runInit(cmd *cobra.Command, args []string) error {
	outputPath := ".ptxcode"

	// Check if file already exists
	if _, err := os.Stat(outputPath); err == nil {
		return fmt.Errorf("configuration file already exists: %s", outputPath)
	}

	// Get template content
	content, err := config.GetTemplate(initTemplate)
	if err != nil {
		return fmt.Errorf("unknown template: %s", initTemplate)
	}

	// Write file
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write configuration: %w", err)
	}

	absPath, _ := filepath.Abs(outputPath)
	fmt.Printf("Created configuration file: %s\n", absPath)
	fmt.Println("\nNext steps:")
	fmt.Println("  1. Edit .ptxcode to configure your analysis")
	fmt.Println("  2. Run 'ptx-codeanalysis analyze' to start analysis")

	return nil
}
