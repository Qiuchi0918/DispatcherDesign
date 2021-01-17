package com.example.demo.jobnode;

public class TaskNode extends JobNode {
    public TaskNode(int jobId, int nodeId, int taskId) {
        super(jobId, nodeId);
        this.taskId = taskId;
    }

    public final int taskId;
}
