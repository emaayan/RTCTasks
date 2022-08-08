package org.rtctasks;


import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.PermissionDeniedException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.RequestFailedException;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;
import org.rtctasks.core.RTCProject;
import org.rtctasks.model.RTCComment;
import org.rtctasks.model.RTCTask;
import org.rtctasks.model.RTCTaskState;
import org.rtctasks.model.RTCTaskType;

import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
//https://intellij-support.jetbrains.com/hc/en-us/community/posts/207098975-Documenation-about-Task-Api
//https://rsjazz.wordpress.com/2015/03/31/the-work-item-time-tracking-api/
//https://rsjazz.wordpress.com/2012/07/31/rtc-update-parent-duration-estimation-and-effort-participant//
//https://jazz.net/library/article/1229

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 14:54
 */
@Tag(RTCTasksRepositoryType.NAME)
public class RTCTasksRepository extends BaseRepository {

    public final static Logger LOGGER = Logger.getInstance(RTCTasksRepository.class);

    private volatile RTCConnector rtcConnector = null;
    private final Map<Integer, TaskState> taskStateMapper = new HashMap<>();
    private final Map<String, TaskType> taskTypeMapper = new HashMap<>();
    private String projectArea;

    //MUST BE KEPT FOR XML DE-SERIERLIZE!!
    public RTCTasksRepository() {
        super();
    }

    public RTCTasksRepository(final TaskRepositoryType type) {
        super(type);
    }

    public RTCTasksRepository(final RTCTasksRepository rtcTasksRepository) {
        super(rtcTasksRepository);
        setProjectArea(rtcTasksRepository.getProjectArea());
        setProjectId(rtcTasksRepository.getProjectId());
        initConnector();
        setConversions();
    }


    @Override
    public void initializeRepository() {
        super.initializeRepository();

        LOGGER.info("Init repository "+ this.getUrl() );
        initConnector();
        setConversions();
    }

    protected void setConversions() {
        taskStateMapper.put(IWorkflowInfo.OPEN_STATES, TaskState.OPEN);
        taskStateMapper.put(IWorkflowInfo.CLOSED_STATES, TaskState.RESOLVED);
        taskStateMapper.put(IWorkflowInfo.IN_PROGRESS_STATES, TaskState.IN_PROGRESS);

        taskTypeMapper.put("defect", TaskType.BUG);
        taskTypeMapper.put("com.ibm.team.workitem.workItemType.defect", TaskType.BUG);
        taskTypeMapper.put("task", TaskType.FEATURE);
        taskTypeMapper.put("com.ibm.team.workitem.workItemType.task", TaskType.FEATURE);
        taskTypeMapper.put("", TaskType.OTHER);
    }


    private void initConnector() {
        if (isConfigured()) {
            final String url = getUrl();
            final String username = getUsername();
            final String password = getPassword();
            rtcConnector =new RTCConnector(url, username, password);
        }
    }

    private TaskState getTaskState(int state) {
        return taskStateMapper.getOrDefault(state, TaskState.OTHER);
    }

    private TaskType getTaskType(String workItemType) {
        return taskTypeMapper.getOrDefault(workItemType, TaskType.OTHER);
    }

    @Override
    public boolean isShouldFormatCommitMessage() {
        return true;
    }

    @Override
    public Task[] getIssues(@Nullable final String query, final int offset, final int limit, final boolean withClosed, @NotNull final ProgressIndicator cancelled) {
        LOGGER.info("Query is " + query);
        final Optional<RTCConnector> connector = getConnector();
        final ProgressMonitor progressMonitor=new ProgressMonitor(cancelled);
        return connector.map(rtcConnector -> {
            final Task[] tasks1;
            try {
                final RTCProject project = rtcConnector.getProject(projectId,progressMonitor);
                if (isNumber(query)) {
                    final int id = Integer.parseInt(query);
                    try {
                        final IWorkItem workItem = rtcConnector.getJazzWorkItemById(id, progressMonitor);
                        if (workItem!=null) {
                            final RTCTask workItemBy = getRtcTask(rtcConnector, project, workItem, progressMonitor);
                            tasks1 = new Task[]{workItemBy};
                        }else{
                            tasks1 = new Task[]{};
                        }
                    } catch (PermissionDeniedException e) {
                        LOGGER.warn("Couldn't find " + id);//because we can't access some task id
                        return new Task[]{};
                    }
                } else {
                    final List<IWorkItem> workItemsBy = project.getWorkItemsBy(query,progressMonitor);
                    tasks1 = new Task[workItemsBy.size()];
                    for (int i = 0; i < workItemsBy.size(); i++) {
                        final IWorkItem iWorkItem = workItemsBy.get(i);
                        tasks1[i] = getRtcTask(rtcConnector, project, iWorkItem,progressMonitor);
                    }
                }
            }catch (OperationCanceledException e){
                throw new ProcessCanceledException(e);
            } catch (TeamRepositoryException e) {
                if (ExceptionUtil.causedBy(e, ClosedByInterruptException.class)) {
                    throw new ProcessCanceledException(e);
                } else {
                    throw new RequestFailedException(e);
                }
            }
            return tasks1;
        }).orElse(new Task[]{});

    }

    @NotNull
    public RTCTask getRtcTask(RTCConnector rtcConnector, final RTCProject project, IWorkItem workItem,ProgressMonitor progressMonitor) throws TeamRepositoryException {
        final String workItemTypeId = workItem.getWorkItemType();
        final IWorkItemType workItemType = project.getWorkItemType(workItemTypeId);
        final TaskType taskType = getTaskType(workItemTypeId);
        final RTCTaskType rtcTaskType = new RTCTaskType(workItemType, taskType);

        final int taskStateGroup = rtcConnector.getTaskStateGroup(workItem);
        final TaskState taskState = getTaskState(taskStateGroup);
        final boolean isClosed = !project.isOpen(workItem);
        final RTCTaskState rtcTaskState = new RTCTaskState(taskState, isClosed);

        final IComments comments = workItem.getComments();
        final IComment[] contents = comments.getContents();
        final RTCComment[] rtcComments = new RTCComment[contents.length];
        for (int i = 0; i < contents.length; i++) {
            final IComment content = contents[i];
            final IContributorHandle creator = content.getCreator();
            final String contributorName = rtcConnector.getContributorName(creator,progressMonitor);
            final RTCComment rtcComment = new RTCComment(content, contributorName);
            rtcComments[i] = rtcComment;
        }

        return new RTCTask(workItem, rtcTaskType, rtcTaskState, rtcComments, projectArea);
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
    public Task findTask(@NotNull final String s) {
        final String id1 = RTCTask.getId(s);
        final int id = Integer.parseInt(id1);
        final Optional<RTCConnector> connector = getConnector();
        final Optional<Task> task = connector.map(rtcConnector -> {
            try {
                final RTCProject project = rtcConnector.getProject(projectId);
                final IWorkItem jazzWorkItemById = rtcConnector.getJazzWorkItemById(id,null);
                final RTCTask workItemBy = getRtcTask(rtcConnector, project, jazzWorkItemById,null);
                return workItemBy;
            } catch (TeamRepositoryException e) {
                throw new RequestFailedException(e);
            }
        });
        return task.orElse(null);
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
            //private volatile RTCConnector _connector;
            private final NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();

            @Override
            protected void doTest() throws Exception {
                final RTCConnector rtcConnector = new RTCConnector(getUrl(), getUsername(), getPassword(),nullProgressMonitor);
                rtcConnector.login();
            }

            @Override
            public void cancel() {
                nullProgressMonitor.setCanceled(true);
            }
        };

    }

    @Override
    public boolean isConfigured() {
        final boolean isConfigured = super.isConfigured() && hasParameters();
        return isConfigured;
    }

    public boolean hasParameters() {
        return StringUtil.isNotEmpty(this.getUsername())
                && StringUtil.isNotEmpty(this.getPassword())
                && StringUtils.isNotEmpty(this.getProjectArea())
                && StringUtil.isNotEmpty(this.getProjectId());
    }

    public Optional<RTCConnector> getConnector() {
        return Optional.ofNullable(rtcConnector);
    }

    @Tag("projectArea")
    public String getProjectArea() {
        return projectArea;
    }

    public void setProjectArea(String projectArea) {
        this.projectArea = projectArea;
    }

    private String projectId;

    @Tag("projectUUID")
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<IProjectArea> getProjects() throws TeamRepositoryException {
        final RTCConnector rtcConnector=new RTCConnector(getUrl(), getUsername(),getPassword());
        final List<IProjectArea> allProject = rtcConnector.getProjects();
        return allProject;
    }

    public  Map<String,String> getProjectNames(List<IProjectArea> allProject) {
        final Map<String,String> areas=allProject.stream().collect(Collectors.toMap(IProcessArea::getName, iProjectArea -> iProjectArea.getItemId().getUuidValue()));
        return areas;
    }

    public void updateProjectId(String projectName) {
        setProjectArea(projectName);
        if (StringUtil.isNotEmpty(projectName)) {
//            final String projectUid = projectMap.get(projectName);
//            setProjectId(projectUid);
        }else{
            setProjectId("");
        }
    }

    public void updateProjectUID() {
        getConnector().ifPresent(rtcConnector -> {
            final IProjectArea projectsBy;
            try {
                projectsBy = rtcConnector.getProjectsBy(RTCTasksRepository.this.projectArea);
                final String uuidValue = projectsBy.getItemId().getUuidValue();
                setProjectId(uuidValue);
            } catch (TeamRepositoryException e) {
                LOGGER.error("Problem updating project ",e);
            }

        });
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Is supported " + feature + " " + supported);
        }
        return supported;
    }

    @Override  //TaskRepository.TIME_MANAGEMENT
    public void updateTimeSpent(@NotNull LocalTask task, @NotNull String timeSpent, @NotNull String comment)  {
        //super.updateTimeSpent(task, timeSpent, comment);
        final String number = task.getNumber();
        final Optional<RTCConnector> connector = getConnector();
        connector.ifPresent(rtcConnector -> {
            try {
                final IWorkItem jazzWorkItemById = rtcConnector.getJazzWorkItemById(Integer.parseInt(number),null);
                final Duration duration = matchDuration(timeSpent);
                rtcConnector.updateTimeSpent(jazzWorkItemById, duration.toMillis(), comment);
            } catch (TeamRepositoryException e) {
                throw new RequestFailedException(e);
            }
        });
    }

    public static Duration matchDuration(@NotNull String timeSpent) {
        final StringTokenizer stringTokenizer = new StringTokenizer(timeSpent, " ");
        final StringBuffer stringBuffer = new StringBuffer("P");

        boolean startTime = false;
        while (stringTokenizer.hasMoreTokens()) {
            final String s1 = stringTokenizer.nextToken();
            final String s = s1.toUpperCase();
            if (s.endsWith("D")) {
                stringBuffer.append(s);
                if (!startTime && stringTokenizer.hasMoreTokens()) {
                    stringBuffer.append("T");
                    startTime = true;
                }
            } else {
                if (!startTime) {
                    stringBuffer.append("T");
                    startTime = true;
                }
                stringBuffer.append(s);
            }
        }

        final Duration parse = Duration.parse(stringBuffer);
        return parse;
    }

    public static Duration parseDuration(int days, int hours, int minutes) {
        //P2DT3H4M
        String durationPattern = String.format("P%dDT%dH%dM", days, hours, minutes);
        final Duration parse = Duration.parse(durationPattern);
        return parse;
    }

    @NotNull
    @Override //TaskRepository.STATE_UPDATING
    public Set<CustomTaskState> getAvailableTaskStates(@NotNull final Task task) {
        final Optional<RTCConnector> connector = getConnector();
        return connector.map(rtcConnector -> {
            try {
                final RTCProject project = rtcConnector.getProject(projectId);
                final Map<String, String> taskStates1 = project.getTaskStates();
                final Set<Map.Entry<String, String>> entries = taskStates1.entrySet();
                final Set<CustomTaskState> customTaskStates = new HashSet<>();
                for (Map.Entry<String, String> entry : entries) {
                    final String key = entry.getKey();
                    final String value = entry.getValue();
                    final CustomTaskState customTaskState = new CustomTaskState(key, value);
                    customTaskStates.add(customTaskState);
                }
                return customTaskStates;
            } catch (TeamRepositoryException e) {
                throw new RequestFailedException(e);
            }

        }).orElse(Collections.emptySet());
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
