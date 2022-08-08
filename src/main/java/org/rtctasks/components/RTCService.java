package org.rtctasks.components;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskRepositoryType;
import org.rtctasks.core.RTCConnector;


@Service()
public final class RTCService {

    private Project project;

    public RTCService(Project project) {
        this.project = project;
    }

//    public void connect(){
//        final TaskRepository[] allRepositories = TaskManager.getManager(project).getAllRepositories();
//        for (TaskRepository repository : allRepositories) {
//            if (repository instanceof RTCTasksRepository) {
//                final RTCTasksRepository repo = (RTCTasksRepository) repository;
//                final RTCConnector connector = repo.getConnector();
//            }
//        }
//    }
    public void getConnector() {

    }
}
