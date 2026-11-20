// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import "testing"

func TestMatchPattern(t *testing.T) {
	tests := []struct {
		pattern string
		path    string
		want    bool
	}{
		// File name patterns at any depth
		{"**/*.java", "A.java", true},
		{"**/*.java", "src/main/java/demo/A.java", true},
		{"**/*.java", "src/A.ts", false},
		{"**/*.test.ts", "src/app/a.test.ts", true},
		{"**/*.test.ts", "src/app/a.ts", false},
		{"**/test_*.py", "tests/test_a.py", true},
		{"**/test_*.py", "src/a_test.py", false},

		// Directory prefix with ** in the middle
		{"src/**/*", "src/A.java", true},
		{"src/**/*", "src/a/b/A.java", true},
		{"src/**/*", "lib/A.java", false},
		{"src/main/java/**/*.java", "src/main/java/A.java", true},
		{"src/main/java/**/*.java", "src/main/java/demo/x/A.java", true},
		{"src/main/java/**/*.java", "src/test/java/demo/A.java", false},
		{"src/main/java/**/*.java", "other/src/main/java/A.java", false},
		{"src/**/*.tsx", "src/ui/App.tsx", true},
		{"lib/**/*.py", "lib/pkg/mod.py", true},
		{"./src/**/*.py", "src/pkg/mod.py", true},

		// Directory patterns with a trailing **
		{"**/target/**", "target/gen/A.java", true},
		{"**/target/**", "module/target/A.java", true},
		{"**/target/**", "src/targets/A.java", false},
		{"**/node_modules/**", "node_modules/x/A.js", true},
		{"**/__pycache__/**", "pkg/__pycache__/a.py", true},
		{"**/.venv/**", ".venv/lib/a.py", true},

		// Single segment wildcards do not cross directories
		{"src/*.java", "src/A.java", true},
		{"src/*.java", "src/demo/A.java", false},
		{"*.java", "src/A.java", false},
		{"A.java", "A.java", true},
		{"src/?.java", "src/A.java", true},
		{"src/[AB].java", "src/B.java", true},

		// Repeated ** and invalid patterns
		{"**/**/*.java", "a/b/A.java", true},
		{"**", "a/b/A.java", true},
		{"src/[.java", "src/[.java", false},
	}

	for _, tt := range tests {
		t.Run(tt.pattern+"|"+tt.path, func(t *testing.T) {
			if got := matchPattern(tt.pattern, tt.path); got != tt.want {
				t.Errorf("matchPattern(%q, %q) = %v, want %v", tt.pattern, tt.path, got, tt.want)
			}
		})
	}
}

func TestMatchDirPattern(t *testing.T) {
	tests := []struct {
		pattern string
		dir     string
		want    bool
	}{
		{"**/target/**", "target", true},
		{"**/target/**", "module/target", true},
		{"**/target/**", "targets", false},
		{"**/node_modules/**", "web/node_modules", true},
		{"**/build/**", "build", true},
		{"**/dist/**", "dist", true},
		{"**/generated/**", "src/main/generated", true},
		{"src/legacy/**", "src/legacy", true},
		{"src/legacy/**", "lib/legacy", false},
		// Patterns without a trailing /** never exclude a directory
		{"**/*.test.ts", "a.test.ts", false},
		{"**/test_*.py", "test_data", false},
		{"**", "src", false},
	}

	for _, tt := range tests {
		t.Run(tt.pattern+"|"+tt.dir, func(t *testing.T) {
			if got := matchDirPattern(tt.pattern, tt.dir); got != tt.want {
				t.Errorf("matchDirPattern(%q, %q) = %v, want %v", tt.pattern, tt.dir, got, tt.want)
			}
		})
	}
}
