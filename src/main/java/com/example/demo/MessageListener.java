package com.example.demo;

import com.example.demo.component.JobManager;
import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
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
public class MessageListener {
    @Autowired
    public MessageListener(JobManager jobManager, AmqpTemplate amqpTemplate) {
        this.jobManager = jobManager;
        this.amqpTemplate = amqpTemplate;
    }

    private final JobManager jobManager;
    private final AmqpTemplate amqpTemplate;

    /**
     * 用于标记条件语句带来的废弃通路，递归直到通路中有其它外来节点接入为止
     *
     * @param jobNodeList    条件节点后续紧接的节点
     * @param sourceNodeList 条件节点本身
     * @param jobStatus      对应的jobStatus
     */
    private static void MarkSucceedingNodeAsUnreachable(JobStatus jobStatus, List<JobNode> jobNodeList, List<JobNode> sourceNodeList) {
        for (JobNode eachNode : jobNodeList) {
            if (sourceNodeList.containsAll(jobStatus.getForegoingNode(eachNode.nodeId))) {
                List<JobNode> newJobNodeList = new ArrayList<>();
                for (JobNode eachSucceedingNode : jobStatus.getSucceedingNode(eachNode.nodeId)) {
                    if (!newJobNodeList.contains(eachSucceedingNode)) {
                        newJobNodeList.add(eachSucceedingNode);
                        eachSucceedingNode.sourceNodeReachingFlagMap.put(eachNode.nodeId, false);
                    }
                    if (!sourceNodeList.contains(eachSucceedingNode))
                        sourceNodeList.add(eachSucceedingNode);
                }
                MarkSucceedingNodeAsUnreachable(jobStatus, newJobNodeList, sourceNodeList);
            }
        }
    }

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


                    //1、复制参数到jobStatus.parameterStorage中后续节点对应位置
                    //2、发送完成状态到后续节点，需要设置所有不会走的节点
                    //3、将完成消息推送至MQ
                }
                if (eachNode instanceof WaitNode) {
                    //1、等待
                    //2、复制参数到jobStatus.parameterStorage中后续节点对应位置
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
                if (eachNode instanceof LoopStartNode) {
                    LoopStartNode loopStartNode = (LoopStartNode) eachNode;
                    switch (loopStartNode.loopType) {
                        case ITERATION:
                            break;
                        case REPETITION:
                            break;
                    }
                    if (loopStartNode.loopParamFeeder == null) {
                        //第一次进入循环
                    }
                    //1、挑选当前循环使用的参数
                    //2、复制当前参数到jobStatus.parameterStorage中后续节点对应位置
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
                if (eachNode instanceof LoopEndNode) {
                    //1、查询循环起始节点，判断是否还需继续循环
                    //2、挑选当前循环使用的参数
                    //3、发送完成状态到后续节点
                    //4、将完成消息推送至MQ
                }
            }
        }
    }
}
