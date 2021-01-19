package com.example.demo.component;

public class JobEdge {
    public JobEdge(int jobId, int edgeId, int startNodeId, int endNodeId) {
        this.endNodeId = endNodeId;
        this.startNodeId = startNodeId;
        this.edgeId = edgeId;
        this.jobId = jobId;
    }

    public final int edgeId;
    public final int startNodeId;
    public final int endNodeId;
    public final int jobId;
}
