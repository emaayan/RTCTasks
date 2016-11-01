package org.rtctasks;


import com.ibm.team.repository.common.PermissionDeniedException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.CustomTaskState;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepositoryType;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.Set;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:54
 */
@Tag(RTCTasksRepositoryType.NAME)
public class RTCTasksRepository extends BaseRepositoryImpl {


    public static final String REGEX = "\\|";

    public RTCTasksRepository() {
        super();
    }

    public RTCTasksRepository(final RTCTasksRepository rtcTasksRepository) {
        super(rtcTasksRepository);
        this.projectArea=rtcTasksRepository.getProjectArea();
    }

    public RTCTasksRepository(final TaskRepositoryType type) {
        super(type);
    }

    @Override
    public Task[] getIssues(@Nullable final String query, final int offset, final int limit, final boolean withClosed, @NotNull final ProgressIndicator cancelled) throws Exception {
        RTCConnector.LOGGER.info("Query is "+query);
        try{
            final Task[] tasksSync = getTasksSync(query);
            return tasksSync;
        }catch (TeamRepositoryException e){
            if (ExceptionUtil.causedBy(e,ClosedByInterruptException.class) || ExceptionUtil.causedBy(e,PermissionDeniedException.class)){
                throw new ProcessCanceledException(e);
            }else{
                throw e;
            }
        }
        //return getTasksAsync(query);
    }


    @NotNull
    @Override
    public Set<CustomTaskState> getAvailableTaskStates(@NotNull final Task task) throws Exception {
        final Set<CustomTaskState> availableTaskStates = super.getAvailableTaskStates(task);
        return availableTaskStates;
    }



    private Task[] getTasksSync(final @Nullable String query) throws TeamRepositoryException {
        return getTasks(query);
    }


    private Task[] getTasks(final @Nullable String query) throws TeamRepositoryException {
        final Task[] tasks;
        final RTCConnector connector = getConnector();
        if (isNumber(query)) {
            final int id = Integer.parseInt(query);
            final IWorkItem workItemBy = connector.getWorkItemBy(id);
            tasks=new Task[]{new RTCTask(workItemBy,this)};
        } else {
            final List<IWorkItem> workItemsBy = connector.getWorkItemsBy(query);
            tasks = new Task[workItemsBy.size()];
            for (int i = 0; i < workItemsBy.size(); i++) {
                tasks[i] = new RTCTask(workItemsBy.get(i),this);
            }

        }
        return tasks;
    }

    private boolean isNumber(final @Nullable String query) {
        try{
            Integer.parseInt(query);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }

    @Nullable
    @Override
    public Task findTask(@NotNull final String s) throws Exception {
        final String id1 = RTCTask.getId(s);
        final int id = Integer.parseInt(id1);
        final IWorkItem workItemBy = getConnector().getWorkItemBy(id);
        return new RTCTask(workItemBy,this);
//        return null;
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        final RTCTasksRepository cloned = new RTCTasksRepository(this.getRepositoryType());
        cloned.setPassword(this.getPassword());
        cloned.setUrl(this.getUrl());
        cloned.setUsername(this.getUsername());
        cloned.setEncodedPassword(this.getEncodedPassword());
        cloned.setCommitMessageFormat(this.getCommitMessageFormat());
        cloned.setShared(this.isShared());
        cloned.setProjectArea(getProjectArea());
        return cloned;
    }

    @Override
    public String extractId(String taskName) {
        return RTCTask.getId(taskName);
    }

    @Override
    public CancellableConnection createCancellableConnection() {

        return new CancellableConnection() {
            private volatile RTCConnector _connector;

            @Override
            protected void doTest() throws Exception {
                _connector = getConnector();
            }

            @Override
            public void cancel() {
                if (_connector != null) {
                    _connector.getMonitor().setCanceled(true);
                }
            }
        };

    }

    public RTCConnector getConnector() {
        final String username = getUsername();
        return RTCConnector.getConnector(getUrl(), username.split(REGEX)[0], getPassword(), getProjectArea());
    }

    public String getProjectArea() {
//        final String[] split = getUsername().split(REGEX);
//        if (split.length > 1) {
//            return split[1];
//        } else {
//            return "";
//        }
        return projectArea;
    }
    private String projectArea="";
    public void setProjectArea(String projectArea){
         this.projectArea=projectArea;
    }
    public boolean isConfigured() {
        return super.isConfigured() && StringUtil.isNotEmpty(this.getUsername()) && StringUtil.isNotEmpty(this.getPassword());
    }
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RTCTasksRepository)) return false;
        if (!super.equals(o)) return false;

        final RTCTasksRepository that = (RTCTasksRepository) o;

        return getProjectArea() != null ? getProjectArea().equals(that.getProjectArea()) : that.getProjectArea() == null;

    }

    @Override
    public int hashCode() {
        return getProjectArea() != null ? getProjectArea().hashCode() : 0;
    }
}