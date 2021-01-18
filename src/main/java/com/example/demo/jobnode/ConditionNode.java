package com.example.demo.jobnode;

import com.example.demo.conditionexpr.BooleanConditionExpression;

import java.util.Map;

public class ConditionNode extends JobNode {
    public ConditionNode(int jobId, int nodeId, int taskId) {
        super(jobId, nodeId);
    }

    public Map<Integer, BooleanConditionExpression> succeedingNodeId_expr_map;
}
