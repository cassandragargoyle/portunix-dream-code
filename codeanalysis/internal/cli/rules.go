package cli

import (
	"fmt"

	"github.com/spf13/cobra"
)

var (
	rulesEngine   string
	rulesLanguage string
)

func newRulesCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "rules",
		Short: "List available analysis rules",
		Long:  `List available rules for the specified engine and language.`,
		RunE:  runRules,
	}

	cmd.Flags().StringVarP(&rulesEngine, "engine", "e", "sonarlint", "analysis engine (sonarlint, tvkcode)")
	cmd.Flags().StringVarP(&rulesLanguage, "language", "l", "", "programming language filter")

	return cmd
}

func runRules(cmd *cobra.Command, args []string) error {
	fmt.Printf("Listing rules for engine: %s\n", rulesEngine)
	if rulesLanguage != "" {
		fmt.Printf("Filtered by language: %s\n", rulesLanguage)
	}

	// TODO: Implement rules listing via bridge
	fmt.Println("\nNote: Rules listing requires SonarLint bridge to be available.")
	fmt.Println("This feature will be fully implemented when bridge integration is complete.")

	return nil
}
