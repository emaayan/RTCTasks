package org.rtctasks.model;

import com.intellij.tasks.TaskState;

public class RTCTaskState {

    private final TaskState taskState;
    private final boolean isClosed;

    public RTCTaskState(TaskState taskState, boolean isClosed) {
        this.taskState = taskState;
        this.isClosed = isClosed;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public String toString() {
        return "RTCTaskState{" +
                "taskState=" + taskState +
                ", isClosed=" + isClosed +
                '}';
    }
}
