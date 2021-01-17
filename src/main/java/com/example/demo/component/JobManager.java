package com.example.demo.component;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JobManager {
    private final Map<Integer, JobStatus> jobId_status_map = new HashMap<>();

    public void addJobStatus(JobStatus jobStatus) {
        jobId_status_map.put(jobStatus.jobId, jobStatus);
    }

    public JobStatus getJobStatusById(Integer jobId) {
        return jobId_status_map.get(jobId);
    }
}
