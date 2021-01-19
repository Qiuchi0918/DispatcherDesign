package com.example.demo.listener;

import com.example.demo.component.JobManager;
import com.example.demo.component.JobStatusCheckMessenger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class JobStatusCheckListener {

    @Autowired
    public JobStatusCheckListener(JobManager jobManager, AmqpTemplate amqpTemplate) {
        this.jobManager = jobManager;
        this.amqpTemplate = amqpTemplate;
    }

    private final JobManager jobManager;
    private final AmqpTemplate amqpTemplate;


    @RabbitListener(queues = "MQ_StatusCheck")
    public void receiveMessage(JobStatusCheckMessenger messenger) throws InterruptedException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.printf("Messenger arrived,checking status of job %s...\n", messenger.jobId);
        Thread.sleep(2000L);
        System.out.printf("[%s]Inspection of job %s complete,sending messenger...\n", df.format(System.currentTimeMillis()), messenger.jobId);

        amqpTemplate.convertAndSend("amq.topic", "StatusCheck", messenger);
    }
}
