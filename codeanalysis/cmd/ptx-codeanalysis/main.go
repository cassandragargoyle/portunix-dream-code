// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package main

import (
	"fmt"
	"os"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/cli"
)

const version = "1.0.0"

func main() {
	if err := cli.Execute(version); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}
