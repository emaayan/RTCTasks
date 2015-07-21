package org.rtctasks;

import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:53
 */
public class RTCRepositoryType extends BaseRepositoryType<RTCRepository> {
    @NotNull
    @Override
    public String getName() {
        return "RTCTasks";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        final Icon icon = new ImageIcon(this.getClass().getClassLoader().getResource("org/rtctasks/RTC_48.png"), "RTCIcon");
        return icon;
    }

    @NotNull
    @Override
    public TaskRepository createRepository() {
        return new RTCRepository(this);
    }

    @Override
    public Class<RTCRepository> getRepositoryClass() {
        return RTCRepository.class;
    }

    @NotNull
    @Override
    public TaskRepositoryEditor createEditor(final RTCRepository repository, final Project project, final Consumer<RTCRepository> changeListener) {
        final Consumer<RTCRepository> myconsumer = new Consumer<RTCRepository>() {
            public void consume(RTCRepository bugzillaTaskRepository) {
                changeListener.consume(bugzillaTaskRepository);
            }
        };
        return new BaseRepositoryEditor<RTCRepository>(project, repository, myconsumer);
    }
}
