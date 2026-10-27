/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
    name = "analyze",
    description = "Run SonarLint analysis on specified files"
)
public class AnalyzeCommand implements Callable<Integer> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Integer call() {
        try {
            // Read JSON request from stdin
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
            );
            String inputJson = reader.lines().collect(Collectors.joining("\n"));

            if (inputJson.isEmpty()) {
                outputError("No input provided");
                return 1;
            }

            // Parse request
            AnalysisRequest request = objectMapper.readValue(inputJson, AnalysisRequest.class);

            // Run analysis
            AnalysisResponse response = runAnalysis(request);

            // Output response
            String outputJson = objectMapper.writeValueAsString(response);
            System.out.println(outputJson);

            return 0;

        } catch (Exception e) {
            outputError("Analysis failed: " + e.getMessage());
            return 1;
        }
    }

    private AnalysisResponse runAnalysis(AnalysisRequest request) {
        AnalysisResponse response = new AnalysisResponse();
        List<AnalysisIssue> issues = new ArrayList<>();

        try {
            SonarLintAnalyzer analyzer = new SonarLintAnalyzer(request);
            issues = analyzer.analyze();
            response.stats.filesAnalyzed = request.files.size();
        } catch (Exception e) {
            response.error = "Analysis engine error: " + e.getMessage();
        }

        response.issues = issues;
        return response;
    }

    private void outputError(String message) {
        try {
            AnalysisResponse response = new AnalysisResponse();
            response.error = message;
            System.out.println(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            System.err.println("{\"error\": \"" + message + "\"}");
        }
    }

    // Request/Response DTOs
    public static class AnalysisRequest {
        public List<String> files = new ArrayList<>();
        public List<String> languages;
        public RulesConfig rules;
    }

    public static class RulesConfig {
        public List<String> disable;
        public List<String> enable;
        public Map<String, String> params;
    }

    public static class AnalysisResponse {
        public List<AnalysisIssue> issues = new ArrayList<>();
        public Stats stats = new Stats();
        public String error;

        public static class Stats {
            public int filesAnalyzed;
        }
    }

    public static class AnalysisIssue {
        public String ruleKey;
        public String message;
        public String filePath;
        public int startLine;
        public int endLine;
        public int startColumn;
        public int endColumn;
        public String severity;
        public String language;
    }
}
