package com.example.demo;

import com.example.demo.component.JobManager;
import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
import com.example.demo.conditionexpr.BooleanConditionExpression;
import com.example.demo.conditionexpr.ForEachExpression;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.*;
import com.example.demo.task.Task;
import com.example.demo.task.TaskExecutor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageListener {
    @Autowired
    public MessageListener(JobManager jobManager, AmqpTemplate amqpTemplate) {
        this.jobManager = jobManager;
        this.amqpTemplate = amqpTemplate;
    }

    private final JobManager jobManager;
    private final AmqpTemplate amqpTemplate;


    @RabbitListener(queues = "queue.ex")
    public void receiveMessage(NodeResultMessenger messenger) {
        //获取一个消息，消息中应包含 1、源JobNode 2、执行结果

        JobStatus jobStatus = jobManager.getJobStatusById(messenger.jobId);
        if (jobStatus == null)
            return;

        List<JobNode> succeedingNodeList = jobStatus.getSucceedingNode(messenger.nodeId);
        //<?>可否单独设置一个线程用来检测所有节点的等待超时
        for (JobNode eachNode : succeedingNodeList) {
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
                        BooleanConditionExpression expression = conditionNode.succeedingNodeId_expr_map.get(eachSucNode.nodeId);
                        expression.setParameter(inParam);
                        if (expression.execute())
                            eachSucNode.sourceNodeReachingFlagMap.put(conditionNode.nodeId, true);
                        else
                            eachSucNode.sourceNodeReachingFlagMap.put(conditionNode.nodeId, false);
                    }
                    NodeResultMessenger newMessenger = new NodeResultMessenger();
                    newMessenger.jobId = jobStatus.jobId;
                    newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                    newMessenger.nodeId = eachNode.nodeId;
                    amqpTemplate.convertAndSend("fanout.ex", "", newMessenger);

                    //1、复制参数到jobStatus.parameterStorage中后续节点对应位置
                    //2、发送完成状态到后续节点，需要设置所有不会走的节点
                    //3、将完成消息推送至MQ
                }
                if (eachNode instanceof WaitNode) {
                    //1、等待
                    //2、复制参数到jobStatus.parameterStorage中对应位置
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
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
                                jobStatus.setOutParamByNodeIdAndParamName(eachNode.nodeId, "_IterationParam_", paramForNewIter);
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = eachNode.nodeId;
                                amqpTemplate.convertAndSend("fanout.ex", "", newMessenger);
                            } else {
                                LoopEndNode corresEndNode = (LoopEndNode) jobStatus.getNodeById(loopStartNode.endNodeId);
                                jobStatus.getSucceedingNode(corresEndNode.nodeId).forEach(sucNode -> {
                                    jobStatus.setNodeReachingStatus(sucNode.nodeId, corresEndNode.nodeId, true);
                                });
                                NodeResultMessenger newMessenger = new NodeResultMessenger();
                                newMessenger.jobId = jobStatus.jobId;
                                newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                                newMessenger.nodeId = corresEndNode.nodeId;
                                amqpTemplate.convertAndSend("fanout.ex", "", newMessenger);
                            }


                            break;
                        case REPETITION:
                            break;
                    }
                    //1、挑选当前循环使用的参数
                    //2、复制当前参数到jobStatus.parameterStorage中后续节点对应位置
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
                if (eachNode instanceof LoopEndNode) {
                    LoopStartNode corresStartNode = (LoopStartNode) jobStatus.getNodeById(((LoopEndNode) eachNode).startNodeId);
                    jobStatus.resetReachFlagAndIteratorInLoop(corresStartNode.nodeId, jobStatus.getAllNodeInLoop(corresStartNode.nodeId, corresStartNode));
                    if (corresStartNode.forEachExpression.hasNext()) {
                        NodeResultMessenger newMessenger = new NodeResultMessenger();
                        JobNode oneOfFgNode = jobStatus.getForegoingNode(corresStartNode.nodeId).get(0);
                        newMessenger.jobId = jobStatus.jobId;
                        newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                        newMessenger.nodeId = oneOfFgNode.nodeId;
                        amqpTemplate.convertAndSend("fanout.ex", "", newMessenger);
                    } else {
                        jobStatus.getSucceedingNode(eachNode.nodeId).forEach(sucNode -> {
                            jobStatus.setNodeReachingStatus(sucNode.nodeId, eachNode.nodeId, true);
                        });
                        NodeResultMessenger newMessenger = new NodeResultMessenger();
                        newMessenger.jobId = jobStatus.jobId;
                        newMessenger.nodeProcessResult = NodeProcessResult.SUCCESS;
                        newMessenger.nodeId = eachNode.nodeId;
                        amqpTemplate.convertAndSend("fanout.ex", "", newMessenger);
                    }

                    //1、查询循环起始节点，判断是否还需继续循环
                    //2、挑选当前循环使用的参数
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
            }
        }
    }
}
