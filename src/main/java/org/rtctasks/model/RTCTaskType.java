package org.rtctasks.model;

import com.ibm.team.workitem.common.model.IWorkItemType;
import com.intellij.tasks.TaskType;
import icons.TasksCoreIcons;
import org.rtctasks.RTCTasksRepositoryType;

import javax.swing.*;

public class RTCTaskType {
    private final IWorkItemType workItemType;
    private final TaskType taskType;

    public RTCTaskType(IWorkItemType workItemType, TaskType taskType) {
        this.workItemType = workItemType;
        this.taskType = taskType;
    }

    public String getDisplayName() {
        return workItemType.getDisplayName();
    }

    public TaskType getTaskType() {
        return taskType;
    }

    @Override
    public String toString() {
        return "RTCTaskType{" +
                "workItemType=" + workItemType +
                ", taskType=" + taskType +
                '}';
    }

    public Icon getIcon() {
        switch (taskType) {
            case BUG:
                return RTCTasksRepositoryType.BUG;
            case FEATURE:
                return RTCTasksRepositoryType.FEATURE;
            default:
                return TasksCoreIcons.Clock;
        }
    }
}
