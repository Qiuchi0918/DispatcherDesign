package com.example.demo.listener;

import com.example.demo.component.JobManager;
import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
import com.example.demo.conditionexpr.ConditionExpression;
import com.example.demo.conditionexpr.ForEachExpression;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.*;
import com.example.demo.task.Task;
import com.example.demo.task.TaskExecutor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NodeResultMessageListener {
    @Autowired
    public NodeResultMessageListener(JobManager jobManager, AmqpTemplate amqpTemplate) {
        this.jobManager = jobManager;
        this.amqpTemplate = amqpTemplate;
    }

    private final JobManager jobManager;
    private final AmqpTemplate amqpTemplate;


    @RabbitListener(queues = "MQ_NodeResult")
    public void receiveMessage(NodeResultMessenger messenger) throws InterruptedException {
        //获取一个消息，消息中应包含 1、源JobNode 2、执行结果

        JobStatus jobStatus = jobManager.getJobStatusById(messenger.jobId);
        if (jobStatus == null)
            return;
        List<JobNode> nodeToBeReachedList = new ArrayList<>();
        if (messenger.selectiveEnterNodeId != -1)
            nodeToBeReachedList.add(jobStatus.getNodeById(messenger.selectiveEnterNodeId));
        else
            nodeToBeReachedList = jobStatus.getSucceedingNode(messenger.nodeId);
        //<?>可否单独设置一个线程用来检测所有节点的等待超时
        for (JobNode eachNode : nodeToBeReachedList) {
            if (eachNode.canProceed()) {
                Map<String, Object> inParam = jobStatus.gatherInParamForNodeById(eachNode.nodeId);
                if (eachNode instanceof TaskNode) {
                    TaskNode curNode = (TaskNode) eachNode;
                    Task curTask = Task.getTaskById(curNode.taskId);
                    TaskExecutor curExecutor = new TaskExecutor(amqpTemplate, curNode, curTask, jobStatus, inParam);
                    curExecutor.start();
                    //在run中应该完成以下工作：
                    //1、任务本身的完成 <?>对于不同的任务类型所传入传出的参数需要用统一容器
                    //2、输出参数发送至jobStatus.parameterStorage对应位置
                    //3、发送出参到后续节点
                    //4、发送完成状态到后续节点
                    //5、将完成消息推送至MQ
                }
                if (eachNode instanceof ConditionNode) {
                    ConditionNode conditionNode = (ConditionNode) eachNode;
                    List<JobNode> succeedingNode = jobStatus.getSucceedingNode(eachNode.nodeId);
                    for (JobNode eachSucNode : succeedingNode) {
                        ConditionExpression expression = conditionNode.succeedingNodeId_expr_map.get(eachSucNode.nodeId);
                        expression.setParameter(inParam);
                        if (expression.execute())
                            eachSucNode.sourceNodeReachingFlagMap.put(conditionNode.nodeId, true);
                        else
                            eachSucNode.sourceNodeReachingFlagMap.put(conditionNode.nodeId, false);
                    }
                    jobStatus.markUnreachable(eachNode);
                    NodeResultMessenger newMessenger = new NodeResultMessenger();
                    newMessenger.jobId = jobStatus.jobId;
                    newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                    newMessenger.nodeId = eachNode.nodeId;
                    amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);

                    //1、复制参数到jobStatus.parameterStorage中后续节点对应位置
                    //2、发送完成状态到后续节点，需要设置所有不会走的节点
                    //3、将完成消息推送至MQ
                }
                if (eachNode instanceof WaitNode) {
                    Thread.sleep(((WaitNode) eachNode).waitTimeInSec * 1000L);
                    for (JobNode eachSucNode : jobStatus.getSucceedingNode(eachNode.nodeId)) {
                        jobStatus.setNodeReachingStatus(eachSucNode.nodeId, eachNode.nodeId, true);
                    }
                    NodeResultMessenger newMessenger = new NodeResultMessenger();
                    newMessenger.jobId = jobStatus.jobId;
                    newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                    newMessenger.nodeId = eachNode.nodeId;
                    amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);
                    //1、等待
                    //2、发送完成状态到后续节点
                    //3、将完成消息推送至MQ
                }
                if (eachNode instanceof LoopStartNode) {
                    LoopStartNode loopStartNode = (LoopStartNode) eachNode;


                    switch (loopStartNode.loopType) {
                        case ITERATION:
                            ForEachExpression forEachExpression = loopStartNode.forEachExpression;
                            if (!forEachExpression.initialized) {
                                forEachExpression.initialize(inParam);
                            }
                            if (forEachExpression.hasNext()) {
                                Object paramForNewIter = forEachExpression.getValueForNewIteration();
                                jobStatus.getSucceedingNode(eachNode.nodeId).forEach(sucNode -> {
                                    jobStatus.setNodeReachingStatus(sucNode.nodeId, eachNode.nodeId, true);
                                });
                                jobStatus.addOutParamByNodeIdAndParamName(eachNode.nodeId, "_IterationParam_", paramForNewIter);
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = eachNode.nodeId;
                                amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);
                            } else {
                                LoopEndNode corresEndNode = (LoopEndNode) jobStatus.getNodeById(loopStartNode.endNodeId);
                                jobStatus.getSucceedingNode(corresEndNode.nodeId).forEach(sucNode -> {
                                    jobStatus.setNodeReachingStatus(sucNode.nodeId, corresEndNode.nodeId, true);
                                });
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = corresEndNode.nodeId;
                                amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);
                            }
                            break;
                        case REPETITION:
                            ConditionExpression conditionExpression = loopStartNode.conditionExpression;
                            if (conditionExpression.execute()) {
                                jobStatus.getSucceedingNode(eachNode.nodeId).forEach(sucNode -> {
                                    jobStatus.setNodeReachingStatus(sucNode.nodeId, eachNode.nodeId, true);
                                });
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = eachNode.nodeId;
                                amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);
                            } else {
                                LoopEndNode corresEndNode = (LoopEndNode) jobStatus.getNodeById(loopStartNode.endNodeId);
                                jobStatus.getSucceedingNode(corresEndNode.nodeId).forEach(sucNode -> {
                                    jobStatus.setNodeReachingStatus(sucNode.nodeId, corresEndNode.nodeId, true);
                                });
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = corresEndNode.nodeId;
                                amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);
                            }

                            break;
                    }
                    //1、挑选当前循环使用的参数
                    //2、复制当前参数到jobStatus.parameterStorage中后续节点对应位置
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
                if (eachNode instanceof LoopEndNode) {
                    LoopStartNode corresStartNode = (LoopStartNode) jobStatus.getNodeById(((LoopEndNode) eachNode).startNodeId);
                    jobStatus.resetReachFlagAndTokenInLoop(corresStartNode.nodeId, jobStatus.getAllNodeInLoop(corresStartNode.nodeId, corresStartNode));
                    NodeResultMessenger newMessenger = new NodeResultMessenger();
                    JobNode oneOfFgNode = jobStatus.getForegoingNode(corresStartNode.nodeId).get(0);
                    newMessenger.jobId = jobStatus.jobId;
                    newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                    newMessenger.nodeId = oneOfFgNode.nodeId;
                    newMessenger.selectiveEnterNodeId = corresStartNode.nodeId;
                    amqpTemplate.convertAndSend("amq.topic", "NodeResult", newMessenger);

                    //1、查询循环起始节点，判断是否还需继续循环
                    //2、挑选当前循环使用的参数
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
            }
        }
    }
}
