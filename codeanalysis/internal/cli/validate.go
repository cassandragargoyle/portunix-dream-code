// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package cli

import (
	"fmt"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
	"github.com/spf13/cobra"
)

func newValidateCmd() *cobra.Command {
	cmd := &cobra.Command{
		Use:   "validate [config-file]",
		Short: "Validate a .ptxcode configuration file",
		Long:  `Validate the syntax and semantics of a .ptxcode configuration file.`,
		Args:  cobra.MaximumNArgs(1),
		RunE:  runValidate,
	}

	return cmd
}

func runValidate(cmd *cobra.Command, args []string) error {
	configPath := ".ptxcode"
	if len(args) > 0 {
		configPath = args[0]
	}

	cfg, err := config.Load(configPath)
	if err != nil {
		fmt.Printf("Invalid configuration: %v\n", err)
		return err
	}

	errors := config.Validate(cfg)
	if len(errors) > 0 {
		fmt.Println("Configuration validation errors:")
		for _, e := range errors {
			fmt.Printf("  - %s\n", e)
		}
		return fmt.Errorf("configuration has %d error(s)", len(errors))
	}

	fmt.Printf("Configuration is valid: %s\n", configPath)
	return nil
}
