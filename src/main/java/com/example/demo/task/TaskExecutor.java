package com.example.demo.task;


import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.JobNode;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.HashMap;
import java.util.Map;

public class TaskExecutor extends Thread {

    private final JobStatus jobStatus;
    private Task task;
    private final JobNode jobNode;
    private final AmqpTemplate amqpTemplate;
    Map<String, Object> inParam;

    public TaskExecutor(AmqpTemplate amqpTemplate, JobNode jobNode, Task task, JobStatus jobStatus, Map<String, Object> inParam) {
        this.task = task;
        this.jobStatus = jobStatus;
        this.jobNode = jobNode;
        this.amqpTemplate = amqpTemplate;
        this.inParam = inParam;
    }


    @Override
    public void run() {
        NodeResultMessenger curExecutionResult = new NodeResultMessenger();
        curExecutionResult.jobId = jobStatus.jobId;
        curExecutionResult.nodeId = jobNode.nodeId;
        curExecutionResult.nodeProcessResult = NodeProcessResult.SUCCESS;
        //从inParam中由参数名获取所需参数
        try {
            //运行...
            System.out.println(task.taskName);
            Thread.sleep(2000L);
            jobStatus.getSucceedingNode(jobNode.nodeId).forEach(eachNode -> {
                jobStatus.setNodeReachingStatus(eachNode.nodeId, jobNode.nodeId, true);
            });
            jobStatus.setOutParamByNodeIdAndParamName(jobNode.nodeId, "paramName", new HashMap<String, Object>());
        } catch (InterruptedException e) {
            curExecutionResult.nodeProcessResult = NodeProcessResult.FAILURE;
            e.printStackTrace();
        }
        amqpTemplate.convertAndSend("fanout.ex", "", curExecutionResult);
    }
}
