/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "rules",
    description = "List available SonarLint rules"
)
public class RulesCommand implements Callable<Integer> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = {"-l", "--language"}, description = "Filter by language")
    private String language;

    @Override
    public Integer call() {
        try {
            RulesResponse response = new RulesResponse();
            response.rules = getAvailableRules();

            if (language != null && !language.isEmpty()) {
                response.rules = response.rules.stream()
                    .filter(r -> r.language.equalsIgnoreCase(language))
                    .toList();
            }

            String outputJson = objectMapper.writeValueAsString(response);
            System.out.println(outputJson);

            return 0;
        } catch (Exception e) {
            System.err.println("{\"error\": \"" + e.getMessage() + "\"}");
            return 1;
        }
    }

    private List<Rule> getAvailableRules() {
        List<Rule> rules = new ArrayList<>();

        // Java rules
        rules.add(new Rule("java:S106", "java", "System.out and System.err should not be used as loggers", "MAJOR"));
        rules.add(new Rule("java:S108", "java", "Nested blocks of code should not be left empty", "MAJOR"));
        rules.add(new Rule("java:S1135", "java", "Track uses of TODO tags", "INFO"));
        rules.add(new Rule("java:S2068", "java", "Credentials should not be hard-coded", "CRITICAL"));

        // JavaScript/TypeScript rules
        rules.add(new Rule("javascript:S2228", "javascript", "Console logging should not be used", "MINOR"));
        rules.add(new Rule("javascript:S1135", "javascript", "Track uses of TODO tags", "INFO"));
        rules.add(new Rule("javascript:S2068", "javascript", "Credentials should not be hard-coded", "CRITICAL"));

        rules.add(new Rule("typescript:S2228", "typescript", "Console logging should not be used", "MINOR"));
        rules.add(new Rule("typescript:S1135", "typescript", "Track uses of TODO tags", "INFO"));
        rules.add(new Rule("typescript:S2068", "typescript", "Credentials should not be hard-coded", "CRITICAL"));

        // Python rules
        rules.add(new Rule("python:S1135", "python", "Track uses of TODO tags", "INFO"));
        rules.add(new Rule("python:S2068", "python", "Credentials should not be hard-coded", "CRITICAL"));

        return rules;
    }

    public static class RulesResponse {
        public List<Rule> rules = new ArrayList<>();
        public String error;
    }

    public static class Rule {
        public String ruleKey;
        public String language;
        public String description;
        public String severity;

        public Rule() {}

        public Rule(String ruleKey, String language, String description, String severity) {
            this.ruleKey = ruleKey;
            this.language = language;
            this.description = description;
            this.severity = severity;
        }
    }
}
