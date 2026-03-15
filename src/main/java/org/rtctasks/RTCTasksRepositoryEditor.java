package org.rtctasks;

import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.tasks.impl.TaskUiUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by exm1110B.
 * Date: 01/11/2016, 12:19
 */
public class RTCTasksRepositoryEditor extends BaseRepositoryEditor<RTCTasksRepository> {
    private final static Logger LOGGER = Logger.getInstance(RTCTasksRepositoryEditor.class);

    private JButton projectButtonPanel;
    private JBLabel projectLabel;
    //private TextFieldWithAutoCompletion<IProjectArea> projectAreaTextField;
    //private JBLabel projectAreaLabel;
    private ComboBoxUpdater projectAreaUpdater;
    public RTCTasksRepositoryEditor(final Project project, final RTCTasksRepository repository, final Consumer<? super RTCTasksRepository> changeListener) {
        super(project, repository, changeListener);
    }

    private static class ComboEntry {
        private final String key;
        private final String value;

        public ComboEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComboEntry that)) return false;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key);
        }
    }
    private class ComboBoxUpdater extends TaskUiUtil.ComboBoxUpdater<ComboEntry> {

        private final Supplier<ComboEntry> selectedItemSupplier;
        private final Consumer<ComboEntry> selectedItemConsumer;
        private final Callable<List<ComboEntry>> onListFetcher;

        ComboBoxUpdater(String title, Supplier<ComboEntry> selectedItemSupplier, Consumer<ComboEntry> selectedItemConsumer, Callable<List<ComboEntry>> onListFetched) {
            super(RTCTasksRepositoryEditor.this.myProject, "Getting " + title, new ComboBox<>(200));
            myComboBox.setRenderer(SimpleListCellRenderer.create("", comboEntry -> comboEntry.value));
            this.selectedItemSupplier = selectedItemSupplier;
            this.selectedItemConsumer = selectedItemConsumer;
            this.onListFetcher = onListFetched;
            installListener(myComboBox);
        }


        @Override
        protected @NotNull List<ComboEntry> fetch(@NotNull ProgressIndicator indicator) throws Exception {
            final List<ComboEntry> projects = onListFetcher.call();
            return new ArrayList<>(projects);
        }


        public void update() {
            final Object selectedItem = getCombo().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem instanceof ComboEntry comboEntry) {
                    selectedItemConsumer.consume(comboEntry);
                } 
            }
        }

        public JComboBox<ComboEntry> getCombo() {
            return myComboBox;
        }

        @Override
        public @Nullable ComboEntry getSelectedItem() {
            return selectedItemSupplier.get();
        }

        @Override
        protected void handleError() {
            super.handleError();
            //       myComboBox.removeAllItems();
        }

    }
    //this gets called in super
    @Override
    @Nullable
    protected JComponent createCustomPanel() {
        this.projectButtonPanel = new JButton("Get Projects");
        projectAreaUpdater = new ComboBoxUpdater("Project area"
                , () -> myRepository.getProjectId() == null ? null : new ComboEntry(myRepository.getProjectId()
                , myRepository.getProjectId()), comboEntry -> myRepository.setProjectId(comboEntry.key), () -> {
            final List<IProjectArea> projects = myRepository.getProjects();
            final List<ComboEntry> projectNames = projects.stream().map(iProjectArea -> new ComboEntry(iProjectArea.getItemId().getUuidValue(), iProjectArea.getName())).collect(Collectors.toList());
            return projectNames;
        });
        this.projectButtonPanel.addActionListener(e -> {
///            final Map<String, String> projectNames = updateProjectNamesInCombo();
            projectAreaUpdater.queue();    
            

        });



        this.projectLabel = new JBLabel("Project area:", 4);


        if (myRepository.hasParameters()) {
            projectAreaUpdater.queue();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
         

            });
        } else {
  
        }
        return FormBuilder.createFormBuilder()
                .addComponent(projectButtonPanel)
                .addLabeledComponent(this.projectLabel, this.projectAreaUpdater.getCombo())
//                .addLabeledComponent("Project area:", this.projectAreaTextField)
//                .addLabeledComponent("UUID:",projectAreaLabel)
                .getPanel();
    }



    public void setAnchor(@Nullable JComponent anchor) {
        super.setAnchor(anchor);
        this.projectLabel.setAnchor(anchor);
    }


    protected void afterTestConnection(boolean connectionSuccessful) {
        super.afterTestConnection(connectionSuccessful);
        if (connectionSuccessful) {
            projectAreaUpdater.queue();

        }
    }

    //this will get called for each key stroke
    public void apply() {
        super.apply();
        projectAreaUpdater.update();

    }
}
