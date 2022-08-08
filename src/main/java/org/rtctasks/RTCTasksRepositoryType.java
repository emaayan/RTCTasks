package org.rtctasks;

import com.ibm.team.repository.common.internal.util.StandaloneExtensionRegistry;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskRepositorySubtype;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.ui.IconManager;
import com.intellij.util.Consumer;

import icons.TasksIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:53
 */
public class RTCTasksRepositoryType extends BaseRepositoryType<RTCTasksRepository> {
    private final static Logger LOGGER =Logger.getInstance(RTCTasksRepositoryType.class);
    public static final String NAME = "RTCTasks";
    private static final Icon RTC_ICON =new ImageIcon(RTCTasksRepositoryType.class.getClassLoader().getResource("icons/RTC_48.png"), "RTCIcon");
    public static final Icon BUG = TasksIcons.Bug;
    public static final Icon FEATURE = new ImageIcon(RTCTasksRepositoryType.class.getClassLoader().getResource("icons/star16.png"),"Feature");

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        final Icon icon = RTC_ICON;
        return icon;
    }

    @NotNull
    @Override
    public TaskRepository createRepository() {
        LOGGER.info("Creating repository");
        final RTCTasksRepository rtcTasksRepository = new RTCTasksRepository(this);
        return rtcTasksRepository;
    }

    @Override
    public Class<RTCTasksRepository> getRepositoryClass() {
        return RTCTasksRepository.class;
    }


    @Override
    public @NotNull TaskRepositoryEditor createEditor(RTCTasksRepository repository, Project project, Consumer<? super RTCTasksRepository> changeListener) {
        final TaskRepositoryEditor rtcTasksRepositoryEditor = new RTCTasksRepositoryEditor(project, repository, changeListener);
        return rtcTasksRepositoryEditor;
    }


}
