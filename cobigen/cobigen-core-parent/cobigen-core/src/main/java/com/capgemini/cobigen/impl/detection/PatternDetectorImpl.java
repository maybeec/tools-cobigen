package com.capgemini.cobigen.impl.detection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capgemini.cobigen.api.PatternDetector;
import com.capgemini.cobigen.api.exception.CobiGenRuntimeException;
import com.capgemini.cobigen.api.to.IncrementTo;
import com.capgemini.cobigen.api.to.PatternMatch;
import com.capgemini.cobigen.impl.config.ConfigurationHolder;

import io.github.maybeec.patterndetection.Detector;

/**
 *
 */
public class PatternDetectorImpl implements PatternDetector {

    /** Logger instance. */
    private static final Logger LOG = LoggerFactory.getLogger(PatternDetectorImpl.class);

    private ConfigurationHolder configurationHolder;

    /**
     * @param configurationHolder
     */
    public PatternDetectorImpl(ConfigurationHolder configurationHolder) {
        this.configurationHolder = configurationHolder;
    }

    @Override
    public List<PatternMatch> detect(IncrementTo increment, Path applicationFilesSearchPath, Path applicationRoot) {

        List<Path> applicationFiles = new ArrayList<>();

        if (applicationFilesSearchPath != null && Files.exists(applicationFilesSearchPath)) {
            if (Files.isRegularFile(applicationFilesSearchPath)) {
                applicationFiles.add(applicationFilesSearchPath);
            } else if (Files.isDirectory(applicationFilesSearchPath)) {
                // TODO let the file ending be configurable by template engine providing the parser
                final PathMatcher matcher = applicationFilesSearchPath.getFileSystem().getPathMatcher("glob:*.java");
                try {
                    applicationFiles.addAll(Files.walk(applicationFilesSearchPath)
                        .filter(e -> Files.isRegularFile(e) && matcher.matches(e)).collect(Collectors.toList()));
                } catch (IOException e) {
                    throw new CobiGenRuntimeException("Could not traverse the application's file tree.", e);
                }
            }
        }
        return detect(increment, applicationFiles, applicationRoot);
    }

    @Override
    public List<PatternMatch> detect(IncrementTo increment, List<Path> applicationFiles, Path applicationRoot) {

        // TODO potential pre-processing? currently we do not cover any filtering of the path to improve
        // performance

        Detector detector = new Detector();
        try {
            return detector
                .detect(configurationHolder
                    .readTemplatesConfiguration(
                        configurationHolder.readContextConfiguration().getTrigger(increment.getTriggerId()))
                    .getIncrement(increment.getId()).getTemplates().parallelStream()
                    .map(e -> e.getAbsoluteTemplatePath()).collect(Collectors.toList()), applicationFiles, ".ftl")
                .parallelStream().map(e -> new PatternMatch(increment, e, null)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new CobiGenRuntimeException("An error occurred on pattern detection.", e);
        }
    }

}
