package com.example.demo.jobnode;

public class WaitNode extends JobNode {
    public WaitNode(int jobId, int nodeId, int taskId, int waitTimeInSec) {
        super(jobId, nodeId);
        this.waitTimeInSec = waitTimeInSec;
    }

    public final int waitTimeInSec;
}
