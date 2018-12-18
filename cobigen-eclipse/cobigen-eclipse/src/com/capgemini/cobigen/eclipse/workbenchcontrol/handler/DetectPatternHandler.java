package com.capgemini.cobigen.eclipse.workbenchcontrol.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.capgemini.cobigen.api.CobiGen;
import com.capgemini.cobigen.api.exception.InvalidConfigurationException;
import com.capgemini.cobigen.api.to.IncrementTo;
import com.capgemini.cobigen.api.to.PatternMatch;
import com.capgemini.cobigen.eclipse.common.exceptions.GeneratorProjectNotExistentException;
import com.capgemini.cobigen.eclipse.common.tools.PlatformUIUtil;
import com.capgemini.cobigen.eclipse.common.tools.ResourcesPluginUtil;
import com.capgemini.cobigen.impl.CobiGenFactory;

/**
 * Handler for the "Detect Patterns..." command in the package explorer
 */
public class DetectPatternHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        ISelection sel = HandlerUtil.getCurrentSelection(event);
        if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
            Object firstItem = ((IStructuredSelection) sel).getFirstElement();
            Path resource = null;
            Path projectPath = null;
            if (firstItem instanceof IResource) {
                resource = Paths.get(((IResource) firstItem).getLocationURI());
                projectPath = Paths.get(((IResource) firstItem).getProject().getLocationURI());
            } else if (firstItem instanceof IJavaElement) {
                resource = Paths.get(((IJavaElement) firstItem).getResource().getLocationURI());
                projectPath = Paths.get(((IJavaElement) firstItem).getJavaProject().getResource().getLocationURI());
            }

            if (resource != null && Files.exists(resource)) {
                try {
                    CobiGen cobigen =
                        CobiGenFactory.create(ResourcesPluginUtil.getGeneratorConfigurationProject().getLocationURI());
                    List<IncrementTo> allIncrements = cobigen.getAllIncrements();

                    IncrementTo selectedIncrement = null;
                    for (IncrementTo increment : allIncrements) {
                        if (increment.getId().equals("logic_impl")) {
                            selectedIncrement = increment;
                            break;
                        }
                    }

                    if (selectedIncrement == null) {
                        MessageDialog.openError(HandlerUtil.getActiveShell(event), "Error",
                            "No increment found with this name");
                        return null;
                    }

                    List<PatternMatch> matches = cobigen.detect(selectedIncrement, resource, projectPath);

                    MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Found!",
                        "The pattern '" + selectedIncrement.getDescription() + "' was found " + matches.size()
                            + " times in the selection with the following variable substitutions:\\n\\n"
                            + toString(matches));
                } catch (InvalidConfigurationException | GeneratorProjectNotExistentException | IOException
                    | CoreException e) {
                    PlatformUIUtil.openErrorDialog("An error occurred", e);
                    e.printStackTrace();
                }
            } else {
                MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Nothing selected",
                    "You have to select a file or folder!");
            }
        }
        return null;
    }

    /**
     * @param matches
     * @return
     */
    private String toString(List<PatternMatch> matches) {
        StringBuilder sb = new StringBuilder();

        int index = 0;
        for (PatternMatch match : matches) {
            sb.append(index);
            sb.append(":\\n");
            for (Entry<String, String> entry : match.getVariableSubstitutions().entrySet()) {
                sb.append(entry.getKey());
                sb.append(" = ");
                sb.append(entry.getValue());
                sb.append("\\n");
            }
            index++;
        }
        return sb.toString();
    }

}
