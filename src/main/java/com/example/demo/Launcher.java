package com.example.demo;


import com.example.demo.component.JobEdge;
import com.example.demo.component.JobManager;
import com.example.demo.component.JobStatus;
import com.example.demo.component.NodeResultMessenger;
import com.example.demo.constant.LoopType;
import com.example.demo.constant.NodeProcessResult;
import com.example.demo.jobnode.JobNode;
import com.example.demo.jobnode.LoopEndNode;
import com.example.demo.jobnode.LoopStartNode;
import com.example.demo.jobnode.TaskNode;
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
        Task.addTask(new Task(0, "Task 0"));
        Task.addTask(new Task(2, "Task 2"));
        Task.addTask(new Task(22, "Task 22"));
        Task.addTask(new Task(2312312, "Task 2312312"));
        Task.addTask(new Task(123, "Task 123"));
        //jobNodeList.add(new TaskNode(0, 0, 0));
        //jobNodeList.add(new TaskNode(0, 1, 2));
        //jobNodeList.add(new TaskNode(0, 2, 22));
        //jobNodeList.add(new TaskNode(0, 3, 2312312));
        //jobNodeList.add(new TaskNode(0, 4, 123));

        jobNodeList.add(new TaskNode(0, 0, 123));
        jobNodeList.get(0).sourceEdgeCount = 0;

        jobNodeList.add(new LoopStartNode(0, 1, LoopType.ITERATION));
        jobNodeList.get(1).sourceEdgeCount = 1;

        jobNodeList.add(new TaskNode(0, 2, 22));
        jobNodeList.get(2).sourceEdgeCount = 1;

        jobNodeList.add(new TaskNode(0, 3, 2312312));
        jobNodeList.get(3).sourceEdgeCount = 1;

        jobNodeList.add(new TaskNode(0, 5, 2));
        jobNodeList.get(4).sourceEdgeCount = 1;

        jobNodeList.add(new LoopEndNode(0, 4, 1, LoopType.ITERATION));
        jobNodeList.get(5).sourceEdgeCount = 2;


        jobEdgeList.add(new JobEdge(0, 0, 1));
        jobEdgeList.add(new JobEdge(1, 1, 2));
        jobEdgeList.add(new JobEdge(2, 2, 3));
        jobEdgeList.add(new JobEdge(3, 3, 4));
        jobEdgeList.add(new JobEdge(4, 1, 5));
        jobEdgeList.add(new JobEdge(5, 5, 4));


        jobNodeList.get(1).sourceNodeReachingFlagMap.put(0, true);
        JobStatus jobStatus = new JobStatus(0, jobNodeList, jobEdgeList);
        NodeResultMessenger messenger = new NodeResultMessenger();
        messenger.nodeProcessResult = NodeProcessResult.SUCCESS;
        messenger.jobId = 0;
        messenger.nodeId = 0;
        jobManager.addJobStatus(jobStatus);
        amqpTemplate.convertAndSend("fanout.ex", "", messenger);
        System.out.println("Launched");
    }
}
