package org.rtctasks;

import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by exm1110B.
 * Date: 01/11/2016, 12:19
 */
public class RTCTasksRepositoryEditor extends BaseRepositoryEditor<RTCTasksRepository> {
    private final static Logger LOGGER = Logger.getInstance(RTCTasksRepositoryEditor.class);

    private JButton projectButtonPanel;
    private JBLabel projectLabel;
    private TextFieldWithAutoCompletion<IProjectArea> projectAreaTextField;
    private JBLabel projectAreaLabel;

    public RTCTasksRepositoryEditor(final Project project, final RTCTasksRepository repository, final Consumer<? super RTCTasksRepository> changeListener) {
        super(project, repository, changeListener);
    }

    //this gets called in super
    @Override
    @Nullable
    protected JComponent createCustomPanel() {
        this.projectButtonPanel = new JButton("Get Projects");
        this.projectButtonPanel.addActionListener(e -> {
            final Map<String, String> projectNames = updateProjectNamesInCombo();
            if (!projectNames.isEmpty() && projectAreaTextField.getText().isEmpty()) {
                final Set<Map.Entry<String, String>> entries = projectNames.entrySet();
                final Map.Entry<String, String> next = entries.iterator().next();
                final String projectName = next.getKey();
                myRepository.updateProjectId(projectName);
            }
        });



        this.projectLabel = new JBLabel("Project area:", 4);
        final String projectArea = this.myRepository.getProjectArea();
        this.projectAreaLabel=new JBLabel("");

        this.projectAreaTextField = new TextFieldWithAutoCompletion<>(this.myProject, new TextFieldWithAutoCompletionListProvider<>(Collections.emptyList()) {
            @Override
            protected @NotNull
            String getLookupString(@NotNull IProjectArea iProjectArea) {
                return iProjectArea.getName();
            }
        }, true, projectArea);
        this.installListener(projectAreaTextField);

        if (myRepository.hasParameters()) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final Map<String, String> stringStringMap = updateProjectNamesInCombo();
                RTCTasksRepositoryEditor.this.projectNames = stringStringMap;
                final String projectAreaName = myRepository.getProjectArea();
                updateProjectId(projectAreaName);
                final String uuid = RTCTasksRepositoryEditor.this.projectNames.get(projectAreaName);
                this.projectAreaLabel.setText(uuid);
            });
        } else {
            this.projectNames = Collections.emptyMap();
            this.projectAreaLabel.setText("");
        }
        return FormBuilder.createFormBuilder()
                .addComponent(projectButtonPanel)
                .addLabeledComponent("Project area:", this.projectAreaTextField)
                .addLabeledComponent("UUID:",projectAreaLabel)
                .getPanel();
    }

    private Map<String, String> projectNames;

    private Map<String, String> updateProjectNamesInCombo() {
        try {
            LOGGER.info("Getting projects");
            final List<IProjectArea> allProject = myRepository.getProjects();
            projectAreaTextField.setVariants(allProject);
            final Map<String, String> projectNames = myRepository.getProjectNames(allProject);
            this.projectNames = projectNames;
            projectAreaTextField.setEnabled(!projectNames.isEmpty());
            return projectNames;
        } catch (TeamRepositoryException e) {
            LOGGER.error("Error getting projects ", e);
            Messages.showErrorDialog(this.myProject, StringUtil.capitalize(e.getMessage()), "Error");
            projectNames = Collections.emptyMap();
            return Collections.emptyMap();
        }

    }


    public void setAnchor(@Nullable JComponent anchor) {
        super.setAnchor(anchor);
        this.projectLabel.setAnchor(anchor);
    }


    protected void afterTestConnection(boolean connectionSuccessful) {
        super.afterTestConnection(connectionSuccessful);
        if (connectionSuccessful) {
           this.updateProjectNamesInCombo();
        }
    }

    //this will get called for each key stroke
    public void apply() {
        super.apply();
        final String projectName = this.projectAreaTextField.getText();
        updateProjectId(projectName);
    }


    private void updateProjectId(String projectName) {
        final String projectId = this.projectNames.getOrDefault(projectName,"");
        this.myRepository.setProjectArea(projectName);
        this.myRepository.setProjectId(projectId);
        this.projectAreaLabel.setText(projectId);
    }


}
