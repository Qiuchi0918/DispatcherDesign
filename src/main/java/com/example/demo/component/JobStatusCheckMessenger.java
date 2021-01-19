package com.example.demo.component;

import java.io.Serializable;

public class JobStatusCheckMessenger implements Serializable {
    public JobStatusCheckMessenger(int jobId) {
        this.jobId = jobId;
    }

    public final int jobId;
}
