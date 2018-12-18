package com.capgemini.cobigen.api;

import java.nio.file.Path;
import java.util.List;

import com.capgemini.cobigen.api.to.IncrementTo;
import com.capgemini.cobigen.api.to.PatternMatch;

/**
 *
 */
public interface PatternDetector {

    public List<PatternMatch> detect(IncrementTo increment, Path applicationFilesSearchPath, Path applicationRoot);

    public List<PatternMatch> detect(IncrementTo increment, List<Path> applicationFiles, Path applicationRoot);
}
