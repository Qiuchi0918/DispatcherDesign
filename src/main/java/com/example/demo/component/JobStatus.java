package com.example.demo.component;


import com.example.demo.jobnode.JobNode;
import com.example.demo.jobnode.LoopEndNode;
import com.example.demo.jobnode.LoopStartNode;

import java.util.*;

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
            idJobNodeMap.get(eachEdge.endNodeId).sourceEdgeCount++;
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


    /**
     * 获取目标节点所有下一级后继节点
     *
     * @param jobNodeId 目标节点id
     * @return 所有下一级后继节点
     */
    public List<JobNode> getSucceedingNode(int jobNodeId) {
        List<JobNode> result = new ArrayList<>();
        idJobEdgeMap.forEach((eachKey, eachEdge) -> {
            if (eachEdge.startNodeId == jobNodeId)
                result.add(getNodeById(eachEdge.endNodeId));
        });
        return result;
    }

    /**
     * 获取目标节点上一级所有节点
     *
     * @param jobNodeId 目标节点
     * @return 所有上一级节点
     */
    public List<JobNode> getForegoingNode(int jobNodeId) {
        List<JobNode> result = new ArrayList<>();
        idJobEdgeMap.forEach((eachKey, eachEdge) -> {
            if (eachEdge.endNodeId == jobNodeId)
                result.add(getNodeById(eachEdge.startNodeId));
        });
        return result;
    }

    /**
     * 向目标节点的出参map添加一组新的命名参数
     *
     * @param nodeId    目标节点
     * @param paramName 参数名称
     * @param param     参数值
     */
    public synchronized void addOutParamByNodeIdAndParamName(int nodeId, String paramName, Object param) {
        if (!nodeIdOutParamNameValueMap.containsKey(nodeId))
            nodeIdOutParamNameValueMap.put(nodeId, new HashMap<>());
        nodeIdOutParamNameValueMap.get(nodeId).put(paramName, param);
    }

    /**
     * 复制所有参数到指定节点对应出参位置
     *
     * @param nodeId      指定节点id
     * @param srcParamMap 来源参数map
     */
    public synchronized void copyAllToNodeOutParamByNodeId(int nodeId, Map<String, Object> srcParamMap) {
        srcParamMap.forEach((k, v) -> {
            addOutParamByNodeIdAndParamName(nodeId, k, v);
        });
    }

    /**
     * 获取目标节点出参
     *
     * @param nodeId 目标节点id
     * @return 目标节点出参
     */
    public synchronized Map<String, Object> getOutParamOfNodeById(int nodeId) {
        if (nodeIdOutParamNameValueMap.containsKey(nodeId))
            return nodeIdOutParamNameValueMap.get(nodeId);
        return null;
    }

    /**
     * 为目标节点，从所有该节点的前置节点获取入参并整合为一个map
     *
     * @param nodeId 目标节点
     * @return 入参
     */
    public synchronized Map<String, Object> gatherInParamForNodeById(int nodeId) {
        Map<String, Object> result = new HashMap<>();
        for (JobNode eachNode : getForegoingNode(nodeId)) {
            Map<String, Object> eachOutParam = getOutParamOfNodeById(eachNode.nodeId);
            if (eachOutParam != null)
                result.putAll(eachOutParam);
        }
        return result;
    }

    /**
     * 设置目标节点接入情况
     *
     * @param curNodeId  目标节点id
     * @param fromNodeId 对应前置节点的id
     * @param isAGo      接入情况
     */
    public synchronized void setNodeReachingStatus(int curNodeId, int fromNodeId, boolean isAGo) {
        JobNode curNode = idJobNodeMap.get(curNodeId);
        curNode.sourceNodeReachingFlagMap.put(fromNodeId, isAGo);
    }

    /**
     * 获取循环内所有节点，包括循环开始和循环结束节点
     *
     * @param startNodeId 循环开始节点id
     * @param starNode    循环开始节点
     * @return 循环内所有节点
     */
    public List<JobNode> getAllNodeInLoop(final int startNodeId, JobNode starNode) {
        List<JobNode> curRecurResult = new ArrayList<>();
        curRecurResult.add(starNode);

        if (starNode instanceof LoopEndNode) {
            LoopEndNode curEndNode = (LoopEndNode) starNode;
            if (curEndNode.startNodeId == startNodeId) {
                return curRecurResult;
            }
        }

        List<JobNode> curSucceedingNodes = getSucceedingNode(starNode.nodeId);
        curSucceedingNodes.forEach(eachSucNode -> {
            List<JobNode> allSucNodeOfThisRecur = getAllNodeInLoop(startNodeId, eachSucNode);
            allSucNodeOfThisRecur.forEach(n -> {
                if (!curRecurResult.contains(n))
                    curRecurResult.add(n);
            });
        });
        return curRecurResult;
    }

    /**
     * 用于检查节点的依赖性，若有边相连则为有依赖
     *
     * @param jobNode    检查目标节点
     * @param dependency 依赖节点
     * @return 若目标节点的母节点不被包含在dependency内，则返回false
     */
    public boolean checkNodeDependency(JobNode jobNode, List<JobNode> dependency) {
        return dependency.containsAll(getForegoingNode(jobNode.nodeId));
    }

    /**
     * 重置循环节内所有节点到达状态和循环起始节点的循环指示器，不会重置其中有连接到循环外节点的相应到达状态
     *
     * @param loopStartNodeID 循环开始节点
     * @param allNodeInLoop   循环内所有节点
     */
    public synchronized void resetReachFlagAndTokenInLoop(int loopStartNodeID, List<JobNode> allNodeInLoop) {
        for (JobNode curNode : allNodeInLoop) {
            if (curNode.nodeId == loopStartNodeID)
                continue;
            else if (curNode instanceof LoopStartNode) {
                ((LoopStartNode) curNode).forEachExpression.initialized = false;
            }

            for (JobNode checker : allNodeInLoop) {
                curNode.sourceNodeReachingFlagMap.remove(checker.nodeId);
            }
        }
    }

    /**
     * 用于标记条件语句带来的废弃通路，使用前要求条件节点已经完成下一级后续节点的连通性设置
     * 不知道有没有问题
     *
     * @param conditionNode 条件节点
     */
    public synchronized void markUnreachable(JobNode conditionNode) {
        List<JobNode> succeedingJobNode = getSucceedingNode(conditionNode.nodeId);
        List<JobNode> appendingLpStartNode = new ArrayList<>();
        for (JobNode eachSucNode : succeedingJobNode) {
            if (eachSucNode.allEdgeReached() && !eachSucNode.canProceed()) {
                if (eachSucNode instanceof LoopStartNode) {
                    LoopEndNode endNode = (LoopEndNode) getNodeById(((LoopStartNode) eachSucNode).endNodeId);
                    appendingLpStartNode.add(endNode);
                    for (JobNode eachSucOfEndNode : getSucceedingNode(endNode.nodeId)) {
                        setNodeReachingStatus(eachSucOfEndNode.nodeId, endNode.nodeId, false);
                    }
                }
                for (JobNode eachSucOfSucNode : getSucceedingNode(eachSucNode.nodeId)) {
                    setNodeReachingStatus(eachSucOfSucNode.nodeId, eachSucNode.nodeId, false);
                }
            }
        }

        succeedingJobNode.addAll(appendingLpStartNode);
        for (JobNode eachSucNode : succeedingJobNode) {
            markUnreachable(eachSucNode);
        }
    }
}
