package com.example.demo.jobnode;

import com.example.demo.conditionexpr.ConditionExpression;
import com.example.demo.conditionexpr.ForEachExpression;
import com.example.demo.constant.LoopType;


public class LoopStartNode extends JobNode {
    public LoopStartNode(int jobId, int nodeId, int endNodeId, LoopType loopType) {
        super(jobId, nodeId);
        this.loopType = loopType;
        this.endNodeId = endNodeId;
    }

    public final int endNodeId;
    public LoopType loopType;
    public ForEachExpression forEachExpression = new ForEachExpression("");
    public ConditionExpression conditionExpression = new ConditionExpression("");
}
