package org.rtctasks;


import com.ibm.team.repository.common.PermissionDeniedException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;
//https://intellij-support.jetbrains.com/hc/en-us/community/posts/207098975-Documenation-about-Task-Api
//https://rsjazz.wordpress.com/2015/03/31/the-work-item-time-tracking-api/
//https://rsjazz.wordpress.com/2012/07/31/rtc-update-parent-duration-estimation-and-effort-participant//
//https://jazz.net/library/article/1229

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:54
 */
@Tag(RTCTasksRepositoryType.NAME)
public class RTCTasksRepository extends BaseRepositoryImpl {
    public final static Logger LOGGER = LogManager.getLogManager().getLogger("global");
    public static final Pattern TIME_SPENT_PATTERN = Pattern.compile("([0-9]+)d ([0-9]+)h ([0-9]+)m");
    public static final String REGEX = "\\|";
    private String projectArea;

    public RTCTasksRepository() {
        super();
        projectArea = "";
    }

    public RTCTasksRepository(final TaskRepositoryType type) {
        super(type);
        this.projectArea = "";
    }

    public RTCTasksRepository(final RTCTasksRepository rtcTasksRepository) {
        super(rtcTasksRepository);
        setRepositoryType(rtcTasksRepository.getRepositoryType());
        setProjectArea(rtcTasksRepository.getProjectArea());
        setUrl(this.getUrl());
        setUsername(this.getUsername());
        setPassword(this.getPassword());
        setEncodedPassword(this.getEncodedPassword());
        setShouldFormatCommitMessage(isShouldFormatCommitMessage());
        setCommitMessageFormat(this.getCommitMessageFormat());
        setShared(this.isShared());
    }

    @Override
    public Task[] getIssues(@Nullable final String query, final int offset, final int limit, final boolean withClosed, @NotNull final ProgressIndicator cancelled) throws Exception {
        RTCConnector.LOGGER.info("Query is " + query);
        try {
            final Task[] tasksSync = getTasksSync(query);
            //RTCConnector.LOGGER.info("Query is " + tasksSync);
//            for (Task task : tasksSync) {
//                RTCConnector.LOGGER.info(task.toString());
//            }
            return tasksSync;
        } catch (TeamRepositoryException e) {
            if (ExceptionUtil.causedBy(e, ClosedByInterruptException.class) || ExceptionUtil.causedBy(e, PermissionDeniedException.class)) {
                throw new ProcessCanceledException(e);
            } else {
                throw e;
            }
        }
        //return getTasksAsync(query);
    }


    private Task[] getTasksSync(final @Nullable String query) throws TeamRepositoryException {
        return getTasks(query);
    }


    private Task[] getTasks(final @Nullable String query) throws TeamRepositoryException {
        final Task[] tasks;
        final RTCConnector connector = getConnector();
        if (isNumber(query)) {
            final int id = Integer.parseInt(query);
            final RTCTask workItemBy = connector.getWorkItemBy(id);
            tasks = new Task[]{workItemBy};
        } else {
            final List<IWorkItem> workItemsBy = connector.getWorkItemsBy(query);
            tasks = new Task[workItemsBy.size()];
            for (int i = 0; i < workItemsBy.size(); i++) {
                tasks[i] = new RTCTask(workItemsBy.get(i), getConnector());
            }

        }
        return tasks;
    }

    private boolean isNumber(final @Nullable String query) {
        try {
            Integer.parseInt(query);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Nullable
    @Override
    public Task findTask(@NotNull final String s) throws Exception {
        final String id1 = RTCTask.getId(s);
        final int id = Integer.parseInt(id1);
        return getConnector().getWorkItemBy(id);
    }

    @NotNull
    @Override
    public BaseRepository clone() {
        final RTCTasksRepository cloned = new RTCTasksRepository(this);
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
        return projectArea;
    }

    public void setProjectArea(String projectArea) {
        this.projectArea = projectArea;
    }

    @Override
    protected int getFeatures() {
        final int features = super.getFeatures()
                | TaskRepository.TIME_MANAGEMENT
                // | TaskRepository.STATE_UPDATING

                ;
        return features;
    }

    @Override
    public boolean isSupported(int feature) {
        final boolean supported = super.isSupported(feature);
        LOGGER.info("Is supported " + feature + " " + supported);
        return supported;
    }

    @Override  //TaskRepository.TIME_MANAGEMENT
    public void updateTimeSpent(@NotNull LocalTask task, @NotNull String timeSpent, @NotNull String comment) throws Exception {
        //super.updateTimeSpent(task, timeSpent, comment);
        final String number = task.getNumber();
        final RTCConnector connector = getConnector();
        final IWorkItem jazzWorkItemById = connector.getJazzWorkItemById(Integer.parseInt(number));
        final Duration duration = matchDuration(timeSpent);
        connector.updateTimeSpent(jazzWorkItemById,duration.toMillis(),comment);
        
    }

    public static  Duration matchDuration(@NotNull String timeSpent) {
        final StringTokenizer stringTokenizer=new StringTokenizer(timeSpent," ");
        final StringBuffer stringBuffer=new StringBuffer("P");
        
        boolean startTime=false;
        while(stringTokenizer.hasMoreTokens()){
            final String s1 = stringTokenizer.nextToken();
            final String s = s1.toUpperCase();
            if (s.endsWith("D")){
                stringBuffer.append(s);
                if (!startTime && stringTokenizer.hasMoreTokens()){
                    stringBuffer.append("T");
                    startTime=true;
                }
            }else{
                if (!startTime){
                    stringBuffer.append("T");
                    startTime=true;
                }
                stringBuffer.append(s);
            }
        }

        final Duration parse = Duration.parse(stringBuffer);
        return parse;
    }

    public static  Duration parseDuration(int days, int hours, int minutes) {
        //P2DT3H4M
        String durationPattern=String.format("P%dDT%dH%dM",days,hours,minutes);
        final Duration parse = Duration.parse(durationPattern);
        return parse;
    }


    @NotNull
    @Override //TaskRepository.STATE_UPDATING
    public Set<CustomTaskState> getAvailableTaskStates(@NotNull final Task task) throws Exception {
        final Set<CustomTaskState> taskStates = getConnector().getTaskStates();
        //        final Set<CustomTaskState> availableTaskStates = super.getAvailableTaskStates(task);
        //        return availableTaskStates;
        return taskStates;
    }

    @Override
    public void setTaskState(@NotNull Task task, @NotNull CustomTaskState state) throws Exception {
        super.setTaskState(task, state);
    }

    public boolean isConfigured() {
        final boolean isConfigured = super.isConfigured() && StringUtil.isNotEmpty(this.getUsername()) && StringUtil.isNotEmpty(this.getPassword());
        return isConfigured;
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