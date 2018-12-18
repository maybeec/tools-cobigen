package com.capgemini.cobigen.api.to;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** One match of a pattern (resp. increment) found in the application's source code. */
public class PatternMatch {

    private IncrementTo pattern;

    private Map<String, String> variableSubstitutions = new HashMap<>();

    private Map<TemplateTo, Path> templateMatches = new HashMap<>();

    /**
     * @param pattern
     * @param variableSubstitutions
     * @param templateMatches
     */
    public PatternMatch(IncrementTo pattern, Map<String, String> variableSubstitutions,
        Map<TemplateTo, Path> templateMatches) {
        super();
        this.pattern = pattern;
        this.variableSubstitutions = variableSubstitutions;
        this.templateMatches = templateMatches;
    }

    public IncrementTo getPattern() {
        return pattern;
    }

    public Map<String, String> getVariableSubstitutions() {
        return variableSubstitutions;
    }

    public Map<TemplateTo, Path> getTemplateMatches() {
        return templateMatches;
    }
}
