// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import "testing"

// engineResults builds engine results with the given issues and file count
func engineResults(filesAnalyzed int, issues ...Issue) *Results {
	r := NewResults()
	for _, issue := range issues {
		r.AddIssue(issue)
	}
	r.Stats.FilesAnalyzed = filesAnalyzed
	return r
}

func TestMergeCombinesIssues(t *testing.T) {
	results := NewResults()
	results.Merge(engineResults(1,
		Issue{RuleID: "S1", Severity: "major", Engine: "sonarlint"},
		Issue{RuleID: "S2", Severity: "minor", Engine: "sonarlint"},
	))
	results.Merge(engineResults(1,
		Issue{RuleID: "TODO", Severity: "info", Engine: "codescan"},
	))

	if len(results.Issues) != 3 {
		t.Fatalf("got %d issues, want 3", len(results.Issues))
	}
	if results.Stats.IssuesFound != 3 {
		t.Errorf("IssuesFound = %d, want 3", results.Stats.IssuesFound)
	}
	wantSeverity := map[string]int{"major": 1, "minor": 1, "info": 1}
	for severity, want := range wantSeverity {
		if got := results.Stats.BySeverity[severity]; got != want {
			t.Errorf("BySeverity[%s] = %d, want %d", severity, got, want)
		}
	}
	wantEngine := map[string]int{"sonarlint": 2, "codescan": 1}
	for engine, want := range wantEngine {
		if got := results.Stats.ByEngine[engine]; got != want {
			t.Errorf("ByEngine[%s] = %d, want %d", engine, got, want)
		}
	}
}

func TestMergeKeepsFilesAnalyzed(t *testing.T) {
	results := NewResults()
	results.Stats.FilesAnalyzed = 1

	// Both engines report the same single file
	results.Merge(engineResults(1, Issue{RuleID: "S1", Severity: "major", Engine: "sonarlint"}))
	results.Merge(engineResults(1, Issue{RuleID: "TODO", Severity: "info", Engine: "codescan"}))

	if results.Stats.FilesAnalyzed != 1 {
		t.Errorf("FilesAnalyzed = %d, want 1", results.Stats.FilesAnalyzed)
	}
}

func TestMergeEmptyResults(t *testing.T) {
	results := NewResults()
	results.Stats.FilesAnalyzed = 2
	results.Merge(NewResults())

	if results.Stats.FilesAnalyzed != 2 || results.Stats.IssuesFound != 0 || len(results.Issues) != 0 {
		t.Errorf("unexpected results after empty merge: %+v", results.Stats)
	}
}
