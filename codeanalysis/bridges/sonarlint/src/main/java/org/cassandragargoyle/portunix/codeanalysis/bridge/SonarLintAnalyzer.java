/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.bridge;

import org.cassandragargoyle.portunix.codeanalysis.bridge.AnalyzeCommand.AnalysisIssue;
import org.cassandragargoyle.portunix.codeanalysis.bridge.AnalyzeCommand.AnalysisRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * SonarLint analyzer wrapper
 *
 * Note: This is a simplified implementation for Phase 1.
 * Full SonarLint integration requires proper engine initialization
 * with language plugins which is complex to set up standalone.
 *
 * For Phase 1 MVP, this implementation provides:
 * - Basic file reading and language detection
 * - Placeholder analysis that can be replaced with real SonarLint
 * - Proper JSON communication with the Go orchestrator
 */
public class SonarLintAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(SonarLintAnalyzer.class);

    private final AnalysisRequest request;
    private final Set<String> disabledRules;

    public SonarLintAnalyzer(AnalysisRequest request) {
        this.request = request;
        this.disabledRules = new HashSet<>();
        if (request.rules != null && request.rules.disable != null) {
            disabledRules.addAll(request.rules.disable);
        }
    }

    public List<AnalysisIssue> analyze() {
        List<AnalysisIssue> issues = new ArrayList<>();

        for (String filePath : request.files) {
            try {
                List<AnalysisIssue> fileIssues = analyzeFile(filePath);
                issues.addAll(fileIssues);
            } catch (Exception e) {
                log.warn("Failed to analyze file: {} - {}", filePath, e.getMessage());
            }
        }

        return issues;
    }

    private List<AnalysisIssue> analyzeFile(String filePath) throws IOException {
        List<AnalysisIssue> issues = new ArrayList<>();
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            log.warn("File not found: {}", filePath);
            return issues;
        }

        String language = detectLanguage(filePath);
        if (!isLanguageEnabled(language)) {
            return issues;
        }

        // Read file content
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String[] lines = content.split("\n");

        // Basic analysis patterns (placeholder for real SonarLint)
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNum = i + 1;

            // Check for common code smells
            issues.addAll(checkLine(filePath, language, line, lineNum));
        }

        return issues;
    }

    private List<AnalysisIssue> checkLine(String filePath, String language, String line, int lineNum) {
        List<AnalysisIssue> issues = new ArrayList<>();

        // System.out.println usage in Java
        if ("java".equals(language) && line.contains("System.out.print")) {
            if (!isRuleDisabled("java:S106")) {
                issues.add(createIssue(
                    "java:S106",
                    "Replace this use of System.out with a logger",
                    filePath, lineNum, "MAJOR", language
                ));
            }
        }

        // console.log usage in JavaScript/TypeScript
        if (("javascript".equals(language) || "typescript".equals(language))
            && line.contains("console.log")) {
            if (!isRuleDisabled("javascript:S2228")) {
                issues.add(createIssue(
                    "javascript:S2228",
                    "Remove this logging statement",
                    filePath, lineNum, "MINOR", language
                ));
            }
        }

        // TODO comments
        if (line.contains("TODO") || line.contains("FIXME")) {
            String ruleId = language + ":S1135";
            if (!isRuleDisabled(ruleId)) {
                issues.add(createIssue(
                    ruleId,
                    "Complete the task associated with this TODO comment",
                    filePath, lineNum, "INFO", language
                ));
            }
        }

        // Empty catch blocks in Java
        if ("java".equals(language) && line.matches(".*catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}.*")) {
            if (!isRuleDisabled("java:S108")) {
                issues.add(createIssue(
                    "java:S108",
                    "Either log or rethrow this exception",
                    filePath, lineNum, "MAJOR", language
                ));
            }
        }

        // Hardcoded passwords (basic check)
        if (line.toLowerCase().matches(".*password\\s*=\\s*[\"'].*[\"'].*")) {
            String ruleId = language + ":S2068";
            if (!isRuleDisabled(ruleId)) {
                issues.add(createIssue(
                    ruleId,
                    "Make sure this hardcoded password is not used in production",
                    filePath, lineNum, "CRITICAL", language
                ));
            }
        }

        return issues;
    }

    private AnalysisIssue createIssue(String ruleKey, String message, String filePath,
                                       int line, String severity, String language) {
        AnalysisIssue issue = new AnalysisIssue();
        issue.ruleKey = ruleKey;
        issue.message = message;
        issue.filePath = filePath;
        issue.startLine = line;
        issue.endLine = line;
        issue.startColumn = 1;
        issue.endColumn = 1;
        issue.severity = severity;
        issue.language = language;
        return issue;
    }

    private String detectLanguage(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".rb")) return "ruby";
        if (lower.endsWith(".php")) return "php";
        return "unknown";
    }

    private boolean isLanguageEnabled(String language) {
        if (request.languages == null || request.languages.isEmpty()) {
            return true; // All languages enabled by default
        }
        return request.languages.contains(language);
    }

    private boolean isRuleDisabled(String ruleId) {
        return disabledRules.contains(ruleId);
    }
}
