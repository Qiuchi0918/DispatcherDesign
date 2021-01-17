package com.example.demo.jobnode;

import com.example.demo.constant.LoopType;

import java.util.Iterator;

public class LoopStartNode extends JobNode {
    public LoopStartNode(int jobId, int nodeId, int taskId, LoopType loopType) {
        super(jobId, nodeId);
        this.loopType = loopType;
    }

    public int endNodeId;
    public LoopType loopType;
    public Iterator<Object> loopParamFeeder;
}
