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
public class RTCTasksRepositoryType extends BaseRepositoryType<RTCTasksRepository>  {
    public static final String NAME="RTCTasks";
    @NotNull
    @Override
    public String getName() {
        return NAME;
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
        return new RTCTasksRepository(this);
    }

    @Override
    public Class<RTCTasksRepository> getRepositoryClass() {
        return RTCTasksRepository.class;
    }

    @NotNull
    @Override
    public TaskRepositoryEditor createEditor(final RTCTasksRepository repository, final Project project, final Consumer<RTCTasksRepository> changeListener) {
        final Consumer<RTCTasksRepository> myconsumer = new Consumer<RTCTasksRepository>() {
            public void consume(RTCTasksRepository rtcTaskRepository) {
                changeListener.consume(rtcTaskRepository);
            }
        };
        return new BaseRepositoryEditor<RTCTasksRepository>(project, repository, myconsumer);
    }
    public void hello(){

    }
}
