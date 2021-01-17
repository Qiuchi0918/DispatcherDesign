package com.example.demo.component;


import com.example.demo.jobnode.JobNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobStatus {

    public JobStatus(Integer jobId, List<JobNode> jobNodeList, List<JobEdge> jobEdgeList) {
        this.jobId = jobId;
        idJobNodeMap = new HashMap<>();
        idJobEdgeMap = new HashMap<>();
        nodeIdOutParamNameValueMap = new HashMap<>();

        jobNodeList.forEach(eachNode -> {
            idJobNodeMap.put(eachNode.nodeId, eachNode);
        });
        jobEdgeList.forEach(eachEdge -> {
            idJobEdgeMap.put(eachEdge.edgeId, eachEdge);
        });
    }

    public final int jobId;
    private final Map<Integer, JobNode> idJobNodeMap;
    private final Map<Integer, JobEdge> idJobEdgeMap;
    private final Map<Integer, Map<String, Object>> nodeIdOutParamNameValueMap;

    public JobNode getNodeById(int jobNodeId) {
        return idJobNodeMap.get(jobNodeId);
    }

    public JobEdge getEdgeById(int jobEdgeId) {
        return idJobEdgeMap.get(jobEdgeId);
    }

    public List<JobNode> getSucceedingNode(int jobNodeId) {
        List<JobNode> result = new ArrayList<>();
        idJobEdgeMap.forEach((eachKey, eachEdge) -> {
            if (eachEdge.startNodeId == jobNodeId)
                result.add(getNodeById(eachEdge.endNodeId));
        });
        return result;
    }

    public List<JobNode> getForegoingNode(int jobNodeId) {
        List<JobNode> result = new ArrayList<>();
        idJobEdgeMap.forEach((eachKey, eachEdge) -> {
            if (eachEdge.endNodeId == jobNodeId)
                result.add(getNodeById(eachEdge.endNodeId));
        });
        return result;
    }

    public void setOutParamByNodeIdAndParamName(int nodeId, String paramName, Object param) {
        if (!nodeIdOutParamNameValueMap.containsKey(nodeId))
            nodeIdOutParamNameValueMap.put(nodeId, new HashMap<>());
        nodeIdOutParamNameValueMap.get(nodeId).put(paramName, param);
    }

    public Map<String, Object> getOutParamOfNodeById(int nodeId) {
        if (nodeIdOutParamNameValueMap.containsKey(nodeId))
            return nodeIdOutParamNameValueMap.get(nodeId);
        return null;
    }

    public Map<String, Object> gatherInParamForNodeById(int nodeId) {
        Map<String, Object> result = new HashMap<>();
        for (JobNode eachNode : getForegoingNode(nodeId)) {
            Map<String, Object> eachOutParam = getOutParamOfNodeById(eachNode.nodeId);
            if (eachOutParam != null)
                result.putAll(eachOutParam);
        }
        return result;
    }

    public void setNodeReachingStatus(int curNodeId, int fromNodeId, boolean isAGo) {
        JobNode curNode = idJobNodeMap.get(curNodeId);
        curNode.sourceNodeReachingFlagMap.put(fromNodeId, isAGo);
    }
}
