package com.example.demo.jobnode;


import com.example.demo.constant.LoopType;

public class LoopEndNode extends JobNode {
    public LoopEndNode(int jobId, int nodeId, int startNodeId, LoopType loopType) {
        super(jobId, nodeId);
        this.startNodeId = startNodeId;
    }

    public int startNodeId;
}
