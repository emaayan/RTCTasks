package org.rtctasks;

import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.intellij.tasks.Comment;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 15:10
 */
public class RTCTask extends Task {
    private final IWorkItem _iWorkItem;



    public RTCTask(final IWorkItem iWorkItem) {
        _iWorkItem = iWorkItem;
    }

    public static String getId(String name){
        return name.split("@")[0];
    }

    @NotNull
    @Override
    public String getId() {
        return Integer.toString(_iWorkItem.getId());
    }


    private static void append(StringBuffer sb, String value) {
        if (StringUtils.isNotEmpty(value)) {
            sb.append("@" + value);
        }
    }

    @NotNull
    @Override
    public String getSummary() {
        final StringBuffer sb = new StringBuffer(getId());
        append(sb, getDescription());
        append(sb,_iWorkItem.getPriority().toString());
        append(sb, _iWorkItem.getSeverity().toString());

        return sb.toString();
    }

    @Nullable
    @Override
    public String getDescription() {
        return _iWorkItem.getHTMLDescription().getPlainText();
    }

    @NotNull
    @Override
    public Comment[] getComments() {
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
    }


    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public TaskType getType() {
        return TaskType.BUG;   //TODO: work by itemType
    }

    @Nullable
    @Override
    public Date getUpdated() {
        return _iWorkItem.getRequestedModified();
    }

    @Nullable
    @Override
    public Date getCreated() {
        return _iWorkItem.getCreationDate();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isIssue() {
        return _iWorkItem.getWorkItemType().contains("defect");
    }

    @Nullable
    @Override
    public String getIssueUrl() {
        //https://jazz.net/forum/questions/13336/work-item-url#147114
        return "";
    }


}
