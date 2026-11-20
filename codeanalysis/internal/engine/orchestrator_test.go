// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import (
	"os"
	"path/filepath"
	"sort"
	"testing"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
)

// createTree creates empty files with slash-separated relative paths under root
func createTree(t *testing.T, root string, files []string) {
	t.Helper()
	for _, rel := range files {
		path := filepath.Join(root, filepath.FromSlash(rel))
		if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(path, []byte("// TODO: test\n"), 0o644); err != nil {
			t.Fatal(err)
		}
	}
}

// loadTemplate parses an init template through a temporary .ptxcode file
func loadTemplate(t *testing.T, name string) *config.Config {
	t.Helper()
	content, err := config.GetTemplate(name)
	if err != nil {
		t.Fatal(err)
	}
	return loadConfigContent(t, content)
}

// loadConfigContent parses configuration content through a temporary .ptxcode file
func loadConfigContent(t *testing.T, content string) *config.Config {
	t.Helper()
	path := filepath.Join(t.TempDir(), ".ptxcode")
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
	cfg, err := config.Load(path)
	if err != nil {
		t.Fatal(err)
	}
	return cfg
}

// collectRelative runs collectFiles and returns sorted slash-separated paths relative to root
func collectRelative(t *testing.T, orch *Orchestrator, root, target string) []string {
	t.Helper()
	files, err := orch.collectFiles(target)
	if err != nil {
		t.Fatalf("collectFiles failed: %v", err)
	}
	rels := make([]string, 0, len(files))
	for _, f := range files {
		rel, err := filepath.Rel(root, f)
		if err != nil {
			t.Fatal(err)
		}
		rels = append(rels, filepath.ToSlash(rel))
	}
	sort.Strings(rels)
	return rels
}

func assertFiles(t *testing.T, got, want []string) {
	t.Helper()
	sort.Strings(want)
	if len(got) != len(want) {
		t.Fatalf("got files %v, want %v", got, want)
	}
	for i := range got {
		if got[i] != want[i] {
			t.Fatalf("got files %v, want %v", got, want)
		}
	}
}

// Files present in every template test project, none of them may be analyzed
var noiseFiles = []string{
	"target/gen/A.java",
	"node_modules/x/a.js",
	"build/out/a.js",
	"dist/a.js",
	"README.md",
}

func TestCollectFilesTemplates(t *testing.T) {
	tests := []struct {
		template string
		files    []string
		want     []string
	}{
		{
			template: "default",
			files: []string{
				"src/A.java", "src/app/b.ts", "lib/util.py",
				"src/node_modules/x/a.js", "src/build/a.js", "lib/target/A.java", "src/dist/a.js",
				"other/C.java",
			},
			want: []string{"src/A.java", "src/app/b.ts", "lib/util.py"},
		},
		{
			template: "java",
			files: []string{
				"src/main/java/demo/A.java", "src/test/java/demo/ATest.java",
				"src/main/java/demo/generated/G.java", "src/main/resources/app.properties",
				"module/target/classes/B.java",
			},
			want: []string{"src/main/java/demo/A.java", "src/test/java/demo/ATest.java"},
		},
		{
			template: "typescript",
			files: []string{
				"src/app.ts", "src/ui/App.tsx", "src/legacy/old.js", "src/ui/Button.jsx",
				"src/app.test.ts", "src/ui/App.spec.ts", "src/node_modules/x/a.ts",
				"src/dist/a.js", "test/e2e.ts",
			},
			want: []string{"src/app.ts", "src/ui/App.tsx", "src/legacy/old.js", "src/ui/Button.jsx"},
		},
		{
			template: "python",
			files: []string{
				"src/pkg/mod.py", "lib/helpers.py", "src/pkg/test_mod.py",
				"src/pkg/__pycache__/mod.py", "venv/lib/site.py", ".venv/lib/site.py",
				"src/venv/x.py", "scripts/run.py",
			},
			want: []string{"src/pkg/mod.py", "lib/helpers.py"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.template, func(t *testing.T) {
			root := t.TempDir()
			createTree(t, root, append(tt.files, noiseFiles...))

			orch := NewOrchestrator(loadTemplate(t, tt.template), root)
			assertFiles(t, collectRelative(t, orch, root, root), tt.want)
		})
	}
}

func TestCollectFilesSamplePtxcode(t *testing.T) {
	samplePath := filepath.Join("..", "..", "defaults", "sample.ptxcode")
	cfg, err := config.Load(samplePath)
	if err != nil {
		t.Skipf("sample.ptxcode not found: %v", err)
	}

	root := t.TempDir()
	createTree(t, root, []string{
		"src/main/java/A.java", "src/web/app.ts", "src/web/app.js", "src/tool.py",
		"src/notes.md", "other/B.java",
	})

	orch := NewOrchestrator(cfg, root)
	assertFiles(t, collectRelative(t, orch, root, root),
		[]string{"src/main/java/A.java", "src/web/app.ts", "src/web/app.js", "src/tool.py"})
}

func TestCollectFilesDefaultConfig(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{
		"src/A.java", "app.ts", "lib/util.py", "web/main.js",
		"target/gen/A.java", "node_modules/x/A.java", "build/a.js",
		"module/target/B.java", "README.md",
	})

	// Without .ptxcode the analysis target is the base directory
	orch := NewOrchestrator(config.DefaultConfig(), "")
	assertFiles(t, collectRelative(t, orch, root, root),
		[]string{"src/A.java", "app.ts", "lib/util.py", "web/main.js"})
}

func TestCollectFilesSubdirectoryTarget(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{
		"src/main/java/demo/A.java", "src/main/java/demo/B.java", "src/test/java/demo/ATest.java",
		"target/gen/C.java",
	})

	// Patterns stay relative to the .ptxcode directory when a subdirectory is analyzed
	orch := NewOrchestrator(loadTemplate(t, "java"), root)
	assertFiles(t, collectRelative(t, orch, root, filepath.Join(root, "src", "main")),
		[]string{"src/main/java/demo/A.java", "src/main/java/demo/B.java"})

	// An excluded target directory yields no files
	assertFiles(t, collectRelative(t, orch, root, filepath.Join(root, "target")), []string{})
}

func TestCollectFilesSingleFile(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{
		"src/main/java/demo/A.java", "target/gen/B.java", "src/main/resources/app.properties",
	})
	orch := NewOrchestrator(loadTemplate(t, "java"), root)

	tests := []struct {
		name string
		file string
		want []string
	}{
		{"included file", "src/main/java/demo/A.java", []string{"src/main/java/demo/A.java"}},
		{"excluded file", "target/gen/B.java", []string{}},
		{"not included file", "src/main/resources/app.properties", []string{}},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			target := filepath.Join(root, filepath.FromSlash(tt.file))
			assertFiles(t, collectRelative(t, orch, root, target), tt.want)
		})
	}
}

func TestCollectFilesSingleFileDefaultConfig(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{"A.java", "notes.md"})
	orch := NewOrchestrator(config.DefaultConfig(), "")

	assertFiles(t, collectRelative(t, orch, root, filepath.Join(root, "A.java")), []string{"A.java"})
	assertFiles(t, collectRelative(t, orch, root, filepath.Join(root, "notes.md")), []string{})
}

func TestCollectFilesOutsideBaseDir(t *testing.T) {
	base := t.TempDir()
	project := t.TempDir()
	createTree(t, project, []string{"src/A.java", "target/B.java"})

	// A target outside the base directory is matched relative to the target
	cfg := loadConfigContent(t, `apiVersion: portunix.ai/v1
kind: CodeAnalysis
spec:
  targets:
    include: ["src/**/*.java"]
    exclude: ["**/target/**"]
`)
	orch := NewOrchestrator(cfg, base)
	assertFiles(t, collectRelative(t, orch, project, project), []string{"src/A.java"})
}

// disabledEnginesConfig returns a configuration with the given include patterns and
// both engines disabled, so Run does not need the Java bridges
func disabledEnginesConfig(include ...string) *config.Config {
	cfg := config.DefaultConfig()
	cfg.Spec.Targets.Include = include
	cfg.Spec.Targets.Exclude = nil
	if cfg.Spec.Engines.SonarLint != nil {
		cfg.Spec.Engines.SonarLint.Enabled = false
	}
	if cfg.Spec.Engines.CodeScan != nil {
		cfg.Spec.Engines.CodeScan.Enabled = false
	}
	return cfg
}

func TestRunFilesAnalyzedCount(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{"src/A.java", "src/B.java", "src/notes.md"})

	results, err := NewOrchestrator(disabledEnginesConfig("**/*.java"), root).Run(root)
	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}
	if results.Stats.FilesAnalyzed != 2 {
		t.Errorf("FilesAnalyzed = %d, want 2", results.Stats.FilesAnalyzed)
	}
}

func TestRunFilesAnalyzedNoMatch(t *testing.T) {
	root := t.TempDir()
	createTree(t, root, []string{"src/notes.md"})

	results, err := NewOrchestrator(disabledEnginesConfig("**/*.java"), root).Run(root)
	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}
	if results.Stats.FilesAnalyzed != 0 {
		t.Errorf("FilesAnalyzed = %d, want 0", results.Stats.FilesAnalyzed)
	}
}
