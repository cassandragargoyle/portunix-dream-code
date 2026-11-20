// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package cli

import (
	"encoding/json"
	"os"
	"path/filepath"
	"slices"
	"strings"
	"testing"
)

// Plugin name as used in `portunix <plugin> <command>`
const pluginName = "code-analysis"

type manifestCommand struct {
	Name        string   `json:"name"`
	Description string   `json:"description"`
	Subcommands []string `json:"subcommands"`
	Examples    []string `json:"examples"`
}

type manifest struct {
	Name     string            `json:"name"`
	Commands []manifestCommand `json:"commands"`
}

func loadManifest(t *testing.T) manifest {
	t.Helper()
	// Tests run in the package directory, plugin.json is in the module root
	data, err := os.ReadFile(filepath.Join("..", "..", "plugin.json"))
	if err != nil {
		t.Fatalf("cannot read plugin.json: %v", err)
	}
	var m manifest
	if err := json.Unmarshal(data, &m); err != nil {
		t.Fatalf("invalid plugin.json: %v", err)
	}
	return m
}

// User-facing commands of the CLI, without the generated help and completion commands
func cliCommandNames() []string {
	var names []string
	for _, cmd := range newRootCmd("test").Commands() {
		if cmd.Name() != "help" && cmd.Name() != "completion" {
			names = append(names, cmd.Name())
		}
	}
	return names
}

func TestManifestPluginName(t *testing.T) {
	if m := loadManifest(t); m.Name != pluginName {
		t.Errorf("plugin name = %q, want %q", m.Name, pluginName)
	}
}

func TestManifestListsAllCommands(t *testing.T) {
	m := loadManifest(t)
	var listed []string
	for _, c := range m.Commands {
		listed = append(listed, c.Name)
	}
	for _, name := range cliCommandNames() {
		if !slices.Contains(listed, name) {
			t.Errorf("command missing in plugin.json: %s", name)
		}
	}
}

func TestManifestListsOnlyRealCommands(t *testing.T) {
	cli := cliCommandNames()
	for _, c := range loadManifest(t).Commands {
		if !slices.Contains(cli, c.Name) {
			t.Errorf("command not provided by the CLI: %s", c.Name)
		}
	}
}

func TestManifestHasNoPlaceholderCommand(t *testing.T) {
	// A command entry named after the plugin or the binary is a placeholder, not a command
	for _, c := range loadManifest(t).Commands {
		if c.Name == pluginName || c.Name == "ptx-codeanalysis" {
			t.Errorf("placeholder command in plugin.json: %s", c.Name)
		}
	}
}

func TestManifestCommandsHaveNoSubcommands(t *testing.T) {
	// The CLI commands have no subcommands, init/validate/rules are commands of their own
	for _, c := range loadManifest(t).Commands {
		if len(c.Subcommands) > 0 {
			t.Errorf("command %s lists subcommands %v, the CLI has none", c.Name, c.Subcommands)
		}
	}
}

func TestManifestDescriptionsMatchCli(t *testing.T) {
	root := newRootCmd("test")
	for _, c := range loadManifest(t).Commands {
		cmd, _, err := root.Find([]string{c.Name})
		if err != nil || cmd == root {
			t.Errorf("command not provided by the CLI: %s", c.Name)
			continue
		}
		if c.Description != cmd.Short {
			t.Errorf("description of %s = %q, want %q (CLI short description)", c.Name, c.Description, cmd.Short)
		}
	}
}

func TestManifestExamplesRunTheCommand(t *testing.T) {
	for _, c := range loadManifest(t).Commands {
		prefix := "portunix " + pluginName + " " + c.Name
		for _, example := range c.Examples {
			if example != prefix && !strings.HasPrefix(example, prefix+" ") {
				t.Errorf("example does not start with %q: %s", prefix, example)
				continue
			}
			// The example must resolve to the listed command and its flags must parse
			root := newRootCmd("test")
			args := strings.Fields(strings.TrimPrefix(example, "portunix "+pluginName))
			cmd, rest, err := root.Find(args)
			if err != nil || cmd.Name() != c.Name {
				t.Errorf("example does not run command %s: %s", c.Name, example)
				continue
			}
			if err := cmd.ParseFlags(rest); err != nil {
				t.Errorf("example has invalid flags: %s: %v", example, err)
				continue
			}
			// Effective only for commands that define an argument validator
			if err := cmd.ValidateArgs(cmd.Flags().Args()); err != nil {
				t.Errorf("example has invalid arguments: %s: %v", example, err)
			}
		}
	}
}
