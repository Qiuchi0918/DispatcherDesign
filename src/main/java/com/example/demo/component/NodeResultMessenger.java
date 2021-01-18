package com.example.demo.component;


import com.example.demo.constant.NodeProcessResult;

import java.io.Serializable;

public class NodeResultMessenger implements Serializable {
    public NodeProcessResult nodeProcessResult;
    public int jobId;
    public int nodeId;
    public int selectiveEnterNodeId = -1;
}
