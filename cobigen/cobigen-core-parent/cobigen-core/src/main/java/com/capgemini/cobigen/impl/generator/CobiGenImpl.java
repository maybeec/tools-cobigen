package com.capgemini.cobigen.impl.generator;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.capgemini.cobigen.api.CobiGen;
import com.capgemini.cobigen.api.ConfigurationInterpreter;
import com.capgemini.cobigen.api.InputInterpreter;
import com.capgemini.cobigen.api.PatternDetector;
import com.capgemini.cobigen.api.exception.CobiGenRuntimeException;
import com.capgemini.cobigen.api.exception.InputReaderException;
import com.capgemini.cobigen.api.exception.InvalidConfigurationException;
import com.capgemini.cobigen.api.extension.ModelBuilder;
import com.capgemini.cobigen.api.to.GenerableArtifact;
import com.capgemini.cobigen.api.to.GenerationReportTo;
import com.capgemini.cobigen.api.to.IncrementTo;
import com.capgemini.cobigen.api.to.PatternMatch;
import com.capgemini.cobigen.api.to.TemplateTo;
import com.capgemini.cobigen.impl.annotation.ProxyFactory;
import com.capgemini.cobigen.impl.config.ConfigurationHolder;
import com.capgemini.cobigen.impl.config.ContextConfiguration;
import com.capgemini.cobigen.impl.config.entity.Trigger;
import com.capgemini.cobigen.impl.detection.PatternDetectorImpl;
import com.capgemini.cobigen.impl.model.ModelBuilderImpl;
import com.google.common.collect.Lists;

/**
 * Implementation of the CobiGen API.
 */
public class CobiGenImpl implements CobiGen {

    /** CobiGen Configuration Cache */
    private ConfigurationHolder configurationHolder;

    /**
     * {@link ConfigurationInterpreter} which holds a cache to improve performance for multiple requests for
     * the same input
     */
    private ConfigurationInterpreter configurationInterpreter;

    /**
     * {@link InputInterpreter} which handles InputReader delegates
     */
    private InputInterpreter inputInterpreter;

    private PatternDetector patternDetector;

    /**
     * Creates a new {@link CobiGen} with a given {@link ContextConfiguration}.
     *
     * @param configurationHolder
     *            {@link ConfigurationHolder} holding CobiGen's configuration
     */
    public CobiGenImpl(ConfigurationHolder configurationHolder) {
        this.configurationHolder = configurationHolder;

        // Create proxy of ConfigurationInterpreter to cache method calls
        ConfigurationInterpreterImpl configurationInterpreterProxy =
            ProxyFactory.getProxy(new ConfigurationInterpreterImpl(configurationHolder));
        configurationInterpreter = configurationInterpreterProxy;

        inputInterpreter = ProxyFactory.getProxy(new InputInterpreterImpl(configurationInterpreterProxy));
        patternDetector = ProxyFactory.getProxy(new PatternDetectorImpl(configurationHolder));
    }

    @Override
    public GenerationReportTo generate(Object input, List<? extends GenerableArtifact> generableArtifacts,
        Path targetRootPath) {
        return generate(input, generableArtifacts, targetRootPath, false, null, null);
    }

    @Override
    public GenerationReportTo generate(Object input, List<? extends GenerableArtifact> generableArtifacts,
        Path targetRootPath, boolean forceOverride) {
        return generate(input, generableArtifacts, targetRootPath, forceOverride, null, null);
    }

    @Override
    public GenerationReportTo generate(Object input, List<? extends GenerableArtifact> generableArtifacts,
        Path targetRootPath, boolean forceOverride, List<Class<?>> logicClasses) {
        return generate(input, generableArtifacts, targetRootPath, forceOverride, logicClasses, null);
    }

    @Override
    public GenerationReportTo generate(Object input, List<? extends GenerableArtifact> generableArtifacts,
        Path targetRootPath, boolean forceOverride, List<Class<?>> logicClasses, Map<String, Object> rawModel) {
        Objects.requireNonNull(input, "Input");
        Objects.requireNonNull(generableArtifacts, "List of Artifacts to be generated");
        if (generableArtifacts.contains(null)) {
            throw new CobiGenRuntimeException(
                "A collection of artifacts to be generated has been passed containing null values. "
                    + "Aborting generation, as this has probably not been intended.");
        }
        Objects.requireNonNull(generableArtifacts, "List of Artifacts to be generated");
        Objects.requireNonNull(targetRootPath, "targetRootPath");
        GenerationProcessor gp = new GenerationProcessor(configurationHolder, input, generableArtifacts, targetRootPath,
            forceOverride, logicClasses, rawModel);
        return gp.generate();
    }

    @Override
    public GenerationReportTo generate(Object input, GenerableArtifact generableArtifact, Path targetRootPath) {
        return generate(input, generableArtifact, targetRootPath, false, null, null);
    }

    @Override
    public GenerationReportTo generate(Object input, GenerableArtifact generableArtifact, Path targetRootPath,
        boolean forceOverride) {
        return generate(input, generableArtifact, targetRootPath, forceOverride, null, null);
    }

    @Override
    public GenerationReportTo generate(Object input, GenerableArtifact generableArtifact, Path targetRootPath,
        boolean forceOverride, List<Class<?>> logicClasses) {
        return generate(input, generableArtifact, targetRootPath, forceOverride, logicClasses, null);
    }

    @Override
    public GenerationReportTo generate(Object input, GenerableArtifact generableArtifact, Path targetRootPath,
        boolean forceOverride, List<Class<?>> logicClasses, Map<String, Object> rawModel) {
        Objects.requireNonNull(input, "Input");
        Objects.requireNonNull(generableArtifact, "Artifact to be generated");
        Objects.requireNonNull(targetRootPath, "targetRootPath");
        GenerationProcessor gp = new GenerationProcessor(configurationHolder, input,
            Lists.newArrayList(generableArtifact), targetRootPath, forceOverride, logicClasses, rawModel);
        return gp.generate();
    }

    @Override
    public ModelBuilder getModelBuilder(Object input) {

        List<String> matchingTriggerIds = getMatchingTriggerIds(input);
        // Just take the first trigger as all trigger should have the same input reader. See javadoc.
        Trigger trigger = configurationHolder.readContextConfiguration().getTrigger(matchingTriggerIds.get(0));
        return new ModelBuilderImpl(input, trigger);
    }

    @Override
    public ModelBuilder getModelBuilder(Object generatorInput, String triggerId) {

        Trigger trigger = configurationHolder.readContextConfiguration().getTrigger(triggerId);
        if (trigger == null) {
            throw new IllegalArgumentException("Unknown Trigger with id '" + triggerId + "'.");
        }
        return new ModelBuilderImpl(generatorInput, trigger);
    }

    @Override
    public boolean combinesMultipleInputs(Object input) {
        return inputInterpreter.combinesMultipleInputs(input);
    }

    @Override
    public List<Object> getInputObjectsRecursively(Object input, Charset inputCharset) {
        return inputInterpreter.getInputObjectsRecursively(input, inputCharset);
    }

    @Override
    public List<Object> getInputObjects(Object input, Charset inputCharset) {
        return inputInterpreter.getInputObjects(input, inputCharset);
    }

    @Override
    public Object read(String type, Path path, Charset inputCharset, Object... additionalArguments)
        throws InputReaderException {
        return inputInterpreter.read(type, path, inputCharset, additionalArguments);
    }

    @Override
    public List<IncrementTo> getMatchingIncrements(Object matcherInput) throws InvalidConfigurationException {
        return configurationInterpreter.getMatchingIncrements(matcherInput);
    }

    @Override
    public List<TemplateTo> getMatchingTemplates(Object matcherInput) throws InvalidConfigurationException {
        return configurationInterpreter.getMatchingTemplates(matcherInput);
    }

    @Override
    public List<String> getMatchingTriggerIds(Object matcherInput) {
        return configurationInterpreter.getMatchingTriggerIds(matcherInput);
    }

    @Override
    public Path resolveTemplateDestinationPath(Path targetRootPath, TemplateTo template, Object input) {
        return configurationInterpreter.resolveTemplateDestinationPath(targetRootPath, template, input);
    }

    @Override
    public List<IncrementTo> getAllIncrements() {
        return configurationInterpreter.getAllIncrements();
    }

    @Override
    public List<PatternMatch> detect(IncrementTo increment, List<Path> applicationFiles, Path applicationRoot) {
        return patternDetector.detect(increment, applicationFiles, applicationRoot);
    }

    @Override
    public List<PatternMatch> detect(IncrementTo increment, Path applicationFilesSearchPath, Path applicationRoot) {
        return patternDetector.detect(increment, applicationFilesSearchPath, applicationRoot);
    }

}
