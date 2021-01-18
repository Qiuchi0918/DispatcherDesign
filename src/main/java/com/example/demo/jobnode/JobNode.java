package com.example.demo.jobnode;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class JobNode {
    public JobNode(int jobId, int nodeId) {
        this.nodeId = nodeId;
        this.jobId = jobId;
    }


    public final int jobId;
    public final int nodeId;
    public Timestamp lastVisitTime;
    public int sourceEdgeCount;
    public final Map<Integer, Boolean> sourceNodeReachingFlagMap = new HashMap<>();

    public boolean allEdgeReached() {
        return sourceNodeReachingFlagMap.size() == sourceEdgeCount;
    }

    public boolean canProceed() {
        //判断该节点是否可以执行
        if (sourceEdgeCount != sourceNodeReachingFlagMap.size())
            return false;//还有edge没走到该node
        for (boolean flag : sourceNodeReachingFlagMap.values())
            if (flag)
                return true;//至少存在一个可用的edge链接到该节点
        return false;
    }
}
