package org.rtctasks;

import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by exm1110B.
 * Date: 01/11/2016, 12:19
 */
public class RTCTasksRepositoryEditor extends BaseRepositoryEditor<RTCTasksRepository> {

    private JButton getAreas;
    private JBLabel projectLabel;
    private TextFieldWithAutoCompletion  projectArea;


    public RTCTasksRepositoryEditor(final Project project, final RTCTasksRepository repository, final Consumer<RTCTasksRepository> changeListener) {
        super(project, repository, changeListener);
        this.installListener(projectArea);
        if ((this.myRepository).isConfigured()) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> this.update());
        }
    }

    protected void afterTestConnection(boolean connectionSuccessful) {
        super.afterTestConnection(connectionSuccessful);
        if (connectionSuccessful) {
            this.update();
        }

    }

    public void apply() {
        super.apply();
        final String text = this.projectArea.getText();
        this.myRepository.setProjectArea(text);
    }

    private Collection<String> update() {
        try {
            final List<IProjectArea> allProject = RTCConnector.getAllProject(this.myRepository.getUrl(), this.myRepository.getUsername(), this.myRepository.getPassword());
            final List<String> areas=new ArrayList<>(allProject.size());
            areas.addAll(allProject.stream().map(IProcessArea::getName).collect(Collectors.toList()));
            projectArea.setVariants(areas);
            return areas;
        } catch (TeamRepositoryException e) {
            RTCConnector.LOGGER.severe("Error getting projects "+e);
            Messages.showErrorDialog(this.myProject, StringUtil.capitalize(e.getMessage()), "Error");
            return Collections.EMPTY_LIST;
        }

    }

    @Nullable
    protected JComponent createCustomPanel() {
        this.getAreas=new JButton("Get Projects");
        this.getAreas.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Collection<String> update = update();
                if (!update.isEmpty() && projectArea.getText().isEmpty()){
                    projectArea.setText(update.iterator().next());
                }

            }
        });
        this.projectLabel = new JBLabel("Project Area:", 4);
        this.projectArea= TextFieldWithAutoCompletion.create(this.myProject, Collections.emptyList(), true, ((RTCTasksRepository) this.myRepository).getProjectArea());
        return FormBuilder.createFormBuilder().addComponent(getAreas).addLabeledComponent(this.projectLabel, this.projectArea).getPanel();
    }

    public void setAnchor(@Nullable JComponent anchor) {
        super.setAnchor(anchor);
        this.projectLabel.setAnchor(anchor);
    }
}
