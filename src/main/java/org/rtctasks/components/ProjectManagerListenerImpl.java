package org.rtctasks.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.tasks.TaskManager;
import org.jetbrains.annotations.NotNull;

public class ProjectManagerListenerImpl implements ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(ProjectManagerListenerImpl.class);
    @Override
    public void projectOpened(@NotNull Project project) {
        LOG.info("Starting project");

        final TaskManager manager = TaskManager.getManager(project);

//        System.out.println(project);
//        final TaskRepository[] allRepositories = TaskManager.getManager(project).getAllRepositories();
//        for (TaskRepository repository : allRepositories) {
//
//            final TaskRepositoryType repositoryType = repository.getRepositoryType();
//            if (repository instanceof RTCTasksRepository) {
//                final RTCTasksRepository repo = (RTCTasksRepository) repository;
//
//            }
//
//        }

    }

    @Override
    public void projectClosing(@NotNull Project project) {
        LOG.info(project + " Closed");
    }
}
