// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

// Issue represents a single code analysis issue
type Issue struct {
	RuleID      string `json:"ruleId"`
	Message     string `json:"message"`
	FilePath    string `json:"filePath"`
	StartLine   int    `json:"startLine"`
	EndLine     int    `json:"endLine"`
	StartColumn int    `json:"startColumn,omitempty"`
	EndColumn   int    `json:"endColumn,omitempty"`
	Severity    string `json:"severity"` // blocker, critical, major, minor, info
	Engine      string `json:"engine"`   // sonarlint, tvkcode
	Language    string `json:"language,omitempty"`
}

// Results contains all analysis results
type Results struct {
	Issues []Issue `json:"issues"`
	Stats  Stats   `json:"stats"`
}

// Stats contains analysis statistics
type Stats struct {
	FilesAnalyzed int            `json:"filesAnalyzed"`
	IssuesFound   int            `json:"issuesFound"`
	BySeverity    map[string]int `json:"bySeverity"`
	ByEngine      map[string]int `json:"byEngine"`
}

// NewResults creates a new Results instance
func NewResults() *Results {
	return &Results{
		Issues: make([]Issue, 0),
		Stats: Stats{
			BySeverity: make(map[string]int),
			ByEngine:   make(map[string]int),
		},
	}
}

// AddIssue adds an issue and updates statistics
func (r *Results) AddIssue(issue Issue) {
	r.Issues = append(r.Issues, issue)
	r.Stats.IssuesFound++
	r.Stats.BySeverity[issue.Severity]++
	r.Stats.ByEngine[issue.Engine]++
}

// Merge combines results from another Results instance
func (r *Results) Merge(other *Results) {
	for _, issue := range other.Issues {
		r.AddIssue(issue)
	}
	r.Stats.FilesAnalyzed += other.Stats.FilesAnalyzed
}

// CountBySeverity returns issue counts by severity level
func (r *Results) CountBySeverity() map[string]int {
	return r.Stats.BySeverity
}
