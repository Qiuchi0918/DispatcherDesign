package com.example.demo;


import com.example.demo.component.*;
import com.example.demo.conditionexpr.ConditionExpression;
import com.example.demo.conditionexpr.ForEachExpression;
import com.example.demo.constant.LoopType;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.*;
import com.example.demo.task.Task;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Launcher implements ApplicationListener<ApplicationReadyEvent> {
    private final AmqpTemplate amqpTemplate;
    private final JobManager jobManager;

    @Autowired
    public Launcher(AmqpTemplate amqpTemplate, JobManager jobManager) {
        this.jobManager = jobManager;
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        List<JobNode> jobNodeList = new ArrayList<>();
        List<JobEdge> jobEdgeList = new ArrayList<>();
        Task.addTask(new Task(0, "Task 000"));
        Task.addTask(new Task(1, "Task 111"));
        Task.addTask(new Task(2, "Task 222"));
        Task.addTask(new Task(3, "Task 333"));
        Task.addTask(new Task(4, "Task 444"));
        Task.addTask(new Task(5, "Task 555"));
        Task.addTask(new Task(6, "Task 666"));
        Task.addTask(new Task(7, "Task 777"));
        Task.addTask(new Task(10, "Task 10"));
        Task.addTask(new Task(11, "Task 11"));

        JobNode createNewNode = new EmptyNode(0, 0);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 1, 1);
        createNewNode.sourceNodeReachingFlagMap.put(0, true);
        jobNodeList.add(createNewNode);
        createNewNode = new LoopStartNode(0, 2, 8, LoopType.ITERATION);
        ((LoopStartNode) createNewNode).forEachExpression = new ForEachExpression("OutParamName");
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 3, 3);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 4, 4);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 5, 5);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 6, 6);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 7, 7);
        jobNodeList.add(createNewNode);
        createNewNode = new LoopEndNode(0, 8, 2, LoopType.ITERATION);
        jobNodeList.add(createNewNode);

        createNewNode = new ConditionNode(0, 9);
        ((ConditionNode) createNewNode).succeedingNodeId_expr_map.put(10, new ConditionExpression("count(OutParamName)!=5"));
        ((ConditionNode) createNewNode).succeedingNodeId_expr_map.put(11, new ConditionExpression("count(OutParamName)==5"));
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 10, 10);
        jobNodeList.add(createNewNode);
        createNewNode = new TaskNode(0, 11, 11);
        jobNodeList.add(createNewNode);


        jobEdgeList.add(new JobEdge(0, 0, 0, 1));
        jobEdgeList.add(new JobEdge(0, 1, 1, 2));
        jobEdgeList.add(new JobEdge(0, 2, 2, 3));
        jobEdgeList.add(new JobEdge(0, 3, 2, 4));
        jobEdgeList.add(new JobEdge(0, 4, 3, 5));
        jobEdgeList.add(new JobEdge(0, 5, 4, 6));
        jobEdgeList.add(new JobEdge(0, 6, 4, 7));
        jobEdgeList.add(new JobEdge(0, 7, 7, 8));
        jobEdgeList.add(new JobEdge(0, 8, 5, 8));
        jobEdgeList.add(new JobEdge(0, 9, 6, 8));

        jobEdgeList.add(new JobEdge(0, 10, 8, 9));
        jobEdgeList.add(new JobEdge(0, 11, 9, 10));
        jobEdgeList.add(new JobEdge(0, 12, 9, 11));
        jobEdgeList.add(new JobEdge(0, 13, 1, 9));


        JobStatus jobStatus = new JobStatus(0, jobNodeList, jobEdgeList);
        jobManager.addJobStatus(jobStatus);

        NodeResultMessenger messenger = new NodeResultMessenger();
        messenger.nodeProcessResult = NodeProcessResult.SUCCESS;
        messenger.jobId = 0;
        messenger.nodeId = 0;

        amqpTemplate.convertAndSend("amq.topic", "NodeResult", messenger);

        amqpTemplate.convertAndSend("amq.topic", "StatusCheck", new JobStatusCheckMessenger(0));
        System.out.println("Launched");
    }
}
