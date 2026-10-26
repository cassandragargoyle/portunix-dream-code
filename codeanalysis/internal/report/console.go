package report

import (
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/config"
	"github.com/cassandragargoyle/portunix-plugins/plugins/codeanalysis/internal/engine"
)

// ANSI color codes
const (
	colorReset  = "\033[0m"
	colorRed    = "\033[31m"
	colorYellow = "\033[33m"
	colorBlue   = "\033[34m"
	colorGray   = "\033[90m"
	colorBold   = "\033[1m"
)

// ConsoleReporter outputs analysis results to console
type ConsoleReporter struct {
	config   *config.Config
	useColor bool
}

// NewConsoleReporter creates a new console reporter
func NewConsoleReporter(cfg *config.Config) *ConsoleReporter {
	// Check if output supports color
	useColor := isTerminal()

	return &ConsoleReporter{
		config:   cfg,
		useColor: useColor,
	}
}

// Report outputs the analysis results to console
func (r *ConsoleReporter) Report(results *engine.Results) {
	if len(results.Issues) == 0 {
		r.printSuccess(results)
		return
	}

	r.printHeader(results)
	r.printIssues(results)
	r.printSummary(results)
}

func (r *ConsoleReporter) printSuccess(results *engine.Results) {
	fmt.Println()
	fmt.Printf("%s No issues found%s\n", r.color(colorBold), r.color(colorReset))
	fmt.Printf("Analyzed %d file(s)\n", results.Stats.FilesAnalyzed)
	fmt.Println()
}

func (r *ConsoleReporter) printHeader(results *engine.Results) {
	fmt.Println()
	fmt.Printf("%sCode Analysis Results%s\n", r.color(colorBold), r.color(colorReset))
	fmt.Println(strings.Repeat("-", 50))
}

func (r *ConsoleReporter) printIssues(results *engine.Results) {
	// Group issues by file
	byFile := make(map[string][]engine.Issue)
	for _, issue := range results.Issues {
		byFile[issue.FilePath] = append(byFile[issue.FilePath], issue)
	}

	// Sort files
	files := make([]string, 0, len(byFile))
	for f := range byFile {
		files = append(files, f)
	}
	sort.Strings(files)

	// Print issues by file
	for _, file := range files {
		issues := byFile[file]

		// Sort issues by line
		sort.Slice(issues, func(i, j int) bool {
			return issues[i].StartLine < issues[j].StartLine
		})

		// Print file header
		relPath, _ := filepath.Rel(".", file)
		if relPath == "" {
			relPath = file
		}
		fmt.Printf("\n%s%s%s\n", r.color(colorBold), relPath, r.color(colorReset))

		// Print each issue
		for _, issue := range issues {
			r.printIssue(issue)
		}
	}
}

func (r *ConsoleReporter) printIssue(issue engine.Issue) {
	severityColor := r.getSeverityColor(issue.Severity)
	severityLabel := strings.ToUpper(issue.Severity)

	fmt.Printf("  %s%s%s %s:%d%s %s\n",
		r.color(severityColor),
		severityLabel,
		r.color(colorReset),
		r.color(colorGray),
		issue.StartLine,
		r.color(colorReset),
		issue.Message,
	)

	// Print rule ID
	fmt.Printf("    %s[%s]%s\n",
		r.color(colorGray),
		issue.RuleID,
		r.color(colorReset),
	)
}

func (r *ConsoleReporter) printSummary(results *engine.Results) {
	fmt.Println()
	fmt.Println(strings.Repeat("-", 50))
	fmt.Printf("%sSummary%s\n", r.color(colorBold), r.color(colorReset))
	fmt.Printf("Files analyzed: %d\n", results.Stats.FilesAnalyzed)
	fmt.Printf("Issues found:   %d\n", results.Stats.IssuesFound)

	// Print by severity
	if len(results.Stats.BySeverity) > 0 {
		fmt.Print("By severity:    ")
		parts := []string{}
		for _, sev := range []string{"blocker", "critical", "major", "minor", "info"} {
			if count, ok := results.Stats.BySeverity[sev]; ok && count > 0 {
				color := r.getSeverityColor(sev)
				parts = append(parts, fmt.Sprintf("%s%s: %d%s",
					r.color(color), sev, count, r.color(colorReset)))
			}
		}
		fmt.Println(strings.Join(parts, ", "))
	}

	// Print by engine
	if len(results.Stats.ByEngine) > 0 {
		fmt.Print("By engine:      ")
		parts := []string{}
		for eng, count := range results.Stats.ByEngine {
			parts = append(parts, fmt.Sprintf("%s: %d", eng, count))
		}
		fmt.Println(strings.Join(parts, ", "))
	}

	fmt.Println()
}

func (r *ConsoleReporter) getSeverityColor(severity string) string {
	switch severity {
	case "blocker", "critical":
		return colorRed
	case "major":
		return colorYellow
	case "minor", "info":
		return colorBlue
	default:
		return colorReset
	}
}

func (r *ConsoleReporter) color(code string) string {
	if r.useColor {
		return code
	}
	return ""
}

// isTerminal checks if stdout is a terminal
func isTerminal() bool {
	fileInfo, _ := os.Stdout.Stat()
	return (fileInfo.Mode() & os.ModeCharDevice) != 0
}
