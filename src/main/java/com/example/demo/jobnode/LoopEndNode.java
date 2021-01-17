package com.example.demo.jobnode;


public class LoopEndNode extends JobNode {
    public LoopEndNode(int jobId, int nodeId, int taskId) {
        super(jobId, nodeId);
    }

    public int startNodeId;
}
