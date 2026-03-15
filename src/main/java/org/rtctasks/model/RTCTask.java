package org.rtctasks.model;

import com.ibm.team.repository.client.internal.TeamRepository;
import com.ibm.team.repository.common.Location;
import com.ibm.team.workitem.common.model.*;
import com.intellij.tasks.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.RTCTasksRepository;

import javax.swing.*;
import java.util.Date;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 15:10
 */
public class RTCTask extends Task {

    private final IWorkItem iWorkItem;
    private final RTCTaskType rtcTaskType;
    private final RTCTaskState rtcTaskState;
    private final RTCComment[] rtcComments;
    private final RTCTasksRepository repo;
    private final String projectName;
    public RTCTask(IWorkItem iWorkItem, RTCTaskType rtcTaskType, RTCTaskState rtcTaskState, RTCComment[] rtcComments,String projectName,  RTCTasksRepository repo) {
        this.iWorkItem = iWorkItem;
        this.rtcTaskType = rtcTaskType;
        this.rtcTaskState = rtcTaskState;
        this.rtcComments = rtcComments;
        this.repo=repo;
        this.projectName=projectName;

    }

    public static String getId(String name) {
        //return name;
        return extractNumberFromId( name);
    }
    

    @NotNull
    @Override
    public String getId() {
        final int id = iWorkItem.getId();
        return "%s-%d".formatted(repo.getRepositoryType().getName(), id);
    }

    @Override
    public @Nullable TaskRepository getRepository() {
        return this.repo;
    }

    @NotNull
    @Override
    public String getPresentableId() {
        return this.rtcTaskType.getDisplayName() + " " + getId();
    }

    @Override
    public String getPresentableName() {
        final StringBuffer sb = new StringBuffer(getPresentableId());
        sb.append(" ").append(getSummary());
        return sb.toString();
    }

    @NotNull
    @Override
    public String getSummary() {
        return iWorkItem.getHTMLSummary().getPlainText();
    }

    @Nullable
    @Override
    public String getDescription() {
        return iWorkItem.getHTMLDescription().getPlainText();
    }

    @NotNull
    @Override
    public Comment[] getComments() {
        return rtcComments;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return rtcTaskType.getIcon();
    }


    @NotNull
    @Override
    public TaskType getType() {
        return rtcTaskType.getTaskType();
    }

    @Nullable
    @Override
    public Date getUpdated() {
        return iWorkItem.getRequestedModified();
    }

    @Nullable
    @Override
    public Date getCreated() {
        return iWorkItem.getCreationDate();
    }


    @Nullable
    @Override
    public TaskState getState() {
        return rtcTaskState.getTaskState();
    }

    @Override
    public boolean isClosed() {
        return rtcTaskState.isClosed();
    }

    @Override
    public boolean isIssue() {
        return true;
    }

    @Nullable
    @Override
    public String getProject() {
        return this.projectName;
    }

    @Nullable
    @Override
    public String getIssueUrl() {
        final TeamRepository origin = (TeamRepository) iWorkItem.getOrigin();
        final String repositoryURI = origin.getRepositoryURI();
        final Location location = Location.namedLocation(iWorkItem, repositoryURI);
        final String s = location.toString();
        return s;
    }

}
