package org.rtctasks;

import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepositoryType;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.lang.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import java.util.List;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:54
 */
@Tag("RTCTasks")
public class RTCRepository extends BaseRepositoryImpl {


    public static final String REGEX = "\\|";

    public RTCRepository() {
        super();
    }

    public RTCRepository(final RTCRepository rtcRepository) {
        super(rtcRepository);
    }

    public RTCRepository(final TaskRepositoryType type) {
        super(type);
    }


    @Override
    public Task[] getIssues(@Nullable final String query, final int offset, final int limit, final boolean withClosed, @NotNull final ProgressIndicator cancelled) throws Exception {
        final Task[] tasks;
        if (NumberUtils.isNumber(query)) {
            final IWorkItem workItemBy = getConnector().getWorkItemBy(Integer.parseInt(query));
            if (workItemBy != null) {
                tasks = new Task[]{new RTCTask(workItemBy)};
            } else {
                tasks = RTCTask.createEmpty();
            }
        } else {
            final List<IWorkItem> workItemsBy = getConnector().getWorkItemsBy(query);
            tasks = new Task[workItemsBy.size()];
            for (int i = 0; i < workItemsBy.size(); i++) {
                tasks[i] = new RTCTask(workItemsBy.get(i));
            }

        }
        return tasks;
    }

    @Nullable
    @Override
    public Task findTask(@NotNull final String s) throws Exception {
        final String id1 = RTCTask.getId(s);
        final int id = Integer.parseInt(id1);
        final IWorkItem workItemBy = getConnector().getWorkItemBy(id);
        return new RTCTask(workItemBy);
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        final RTCRepository cloned = new RTCRepository(this.getRepositoryType());
        cloned.setPassword(this.getPassword());
        cloned.setUrl(this.getUrl());
        cloned.setUsername(this.getUsername());
        cloned.setEncodedPassword(this.getEncodedPassword());
        cloned.setCommitMessageFormat(this.getCommitMessageFormat());
        cloned.setShared(this.isShared());
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

    private RTCConnector getConnector() throws TeamRepositoryException {
        final String username = getUsername();
        return RTCConnector.getConnector(getUrl(), username.split(REGEX)[0], getPassword(), getProjectArea());
    }

    public String getProjectArea() {
        final String[] split = getUsername().split(REGEX);
        if (split.length > 1) {
            return split[1];
        } else {
            return "";
        }
    }
}