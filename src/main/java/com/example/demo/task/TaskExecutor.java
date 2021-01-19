package com.example.demo.task;


import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.JobNode;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.ArrayList;
import java.util.List;
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

        Map<String, Object> inParamMap = inParam;
        System.out.println(task.taskName + " start");
        if (inParamMap.containsKey("_IterationParam_")) {
            System.out.println("IterationParam Accepted: " + inParamMap.get("_IterationParam_"));
        }

        try {
            //运行...
            Thread.sleep(1000L);
            jobStatus.getSucceedingNode(jobNode.nodeId).forEach(eachNode -> {
                jobStatus.setNodeReachingStatus(eachNode.nodeId, jobNode.nodeId, true);
            });
            List<String> outParam = new ArrayList<>();
            outParam.add("Param1");
            outParam.add("Param2");
            outParam.add("Param3");
            outParam.add("Param4");
            outParam.add("Param5");
            jobStatus.addOutParamByNodeIdAndParamName(jobNode.nodeId, "OutParamName", outParam);

            //jobStatus.setOutParamByNodeIdAndParamName(jobNode.nodeId, task.outParamName, "");//<?>出参模式

        } catch (InterruptedException e) {
            curExecutionResult.nodeProcessResult = NodeProcessResult.FAILURE;
            e.printStackTrace();
        }
        amqpTemplate.convertAndSend("amq.topic", "NodeResult", curExecutionResult);
        System.out.println(task.taskName + " done, messenger sent");
    }
}
