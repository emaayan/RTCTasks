package org.rtctasks;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.internal.TeamRepository;
import com.ibm.team.repository.common.Location;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.TaskType;
import icons.TasksCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rtctasks.core.RTCConnector;

import javax.swing.*;
import java.util.Date;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 15:10
 */
public class RTCTask extends Task {
    private final IWorkItem _iWorkItem;
    private final RTCConnector _rtcConnector;

    public RTCTask(final IWorkItem iWorkItem, RTCConnector rtcConnector) {
        _iWorkItem = iWorkItem;
        this._rtcConnector = rtcConnector;
    }

    public static String getId(String name) {
        return name.split("@")[0];
    }

    @NotNull
    @Override
    public String getId() {
        if (_iWorkItem != null) {
            final int id = _iWorkItem.getId();
            return Integer.toString(id);
        } else {
            return "";
        }
    }


    private static void append(StringBuffer sb, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("@" + value);
        }
    }

    @NotNull
    @Override
    public String getPresentableId() {
        if (_iWorkItem != null) {
            final IWorkItemType workItemType = getItemType();
            if (workItemType != null) {
                return workItemType.getDisplayName() + " " + getId();
            } else {
                return getId();
            }
        } else {
            return getId();
        }
    }

    @Override
    public String getPresentableName() {
        final StringBuffer sb = new StringBuffer(getPresentableId());
        if (_iWorkItem != null) {
            //append(sb, getSummary());
            sb.append(" ").append(getSummary());
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getSummary() {
        return _iWorkItem != null ? _iWorkItem.getHTMLSummary().getPlainText() : "";
    }

    @Nullable
    @Override
    public String getDescription() {
        return _iWorkItem != null ? _iWorkItem.getHTMLDescription().getPlainText() : "";
    }

    @NotNull
    @Override
    public Comment[] getComments() {
        if (_iWorkItem != null) {
            final IComment[] contents = _iWorkItem.getComments().getContents();
            final Comment[] comments = new Comment[contents.length];
            for (int i = 0; i < contents.length; i++) {
                final IComment content = contents[i];
                comments[i] = new Comment() {
                    @Override
                    public String getText() {
                        return content.getHTMLContent().getPlainText();
                    }

                    @Nullable
                    @Override
                    public String getAuthor() {
                        return content.getCreator().getItemId().toString();
                    }

                    @Nullable
                    @Override
                    public Date getDate() {
                        return content.getCreationDate();
                    }
                };
            }
            return comments;
        } else {
            return new Comment[]{};
        }
    }

    @NotNull
    @Override
    public Icon getIcon() {
        final TaskType type = getType();
        switch (type) {
            case BUG:
                return TasksCoreIcons.Bug;
            case FEATURE:
                return TasksCoreIcons.Feature;
            default:
                return TasksCoreIcons.Clock;
        }
    }

    public String getWorkItemType() {
        return _iWorkItem != null ? _iWorkItem.getWorkItemType() : "";
    }

    public IWorkItemType getItemType() {
        final String workItemType = getWorkItemType();
        return _rtcConnector.getWorkItemType(workItemType);
    }

    /*
    @Nullable
    @Override
    public String getCustomIcon() {
        if (_iWorkItem != null) {
            final IWorkItemType itemType = getItemType();
            if (itemType != null) {
                final URL iconURL = itemType.getIconURL();
                return iconURL.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    } */

    @NotNull
    @Override
    public TaskType getType() {
        return _iWorkItem != null ? _rtcConnector.getTaskType(_iWorkItem) : TaskType.OTHER;
    }

    @Nullable
    @Override
    public Date getUpdated() {
        return _iWorkItem != null ? _iWorkItem.getRequestedModified() : null;
    }

    @Nullable
    @Override
    public Date getCreated() {
        return _iWorkItem != null ? _iWorkItem.getCreationDate() : null;
    }

    @Nullable
    @Override
    public TaskState getState() {
        return _iWorkItem != null ? _rtcConnector.getTaskState(_iWorkItem) : TaskState.OTHER;
    }

    @Override
    public boolean isClosed() {
        return _iWorkItem != null && !_rtcConnector.isOpen(_iWorkItem);
    }

    @Override
    public boolean isIssue() {
        return true;
    }

    @Nullable
    @Override
    public String getProject() {
        final IProjectArea projectArea = _rtcConnector.getProjectArea();
        if (projectArea != null) {
            final String name = projectArea.getName();
            return name;
        } else {
            return super.getProject();
        }
    }

    @Nullable
    @Override
    public String getIssueUrl() {
        if (_iWorkItem != null) {
            final TeamRepository origin = (TeamRepository) _iWorkItem.getOrigin();
            final String repositoryURI = origin.getRepositoryURI();
            final Location location = Location.namedLocation(_iWorkItem, repositoryURI);
            final String s = location.toString();
            return s;
        } else {
            return "";
        }
    }


}
