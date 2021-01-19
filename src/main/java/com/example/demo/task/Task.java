package com.example.demo.task;

import com.example.demo.constant.ParameterType;

import java.util.HashMap;
import java.util.Map;

public class Task {

    public Task(int taskId, String taskName) {
        this.taskName = taskName;
        this.taskId = taskId;
        this.inParamType = ParameterType.BOOLEAN;
        this.outParamType = ParameterType.BOOLEAN;
        this.inParamName = "InParamName";
        this.outParamName = "OutParamName";
    }

    private static final Map<Integer, Task> taskId_task_map = new HashMap<>();

    public static Task getTaskById(int taskId) {
        return taskId_task_map.get(taskId);
    }

    public static void addTask(Task task) {
        taskId_task_map.put(task.taskId, task);
    }

    public final String taskName;
    public final int taskId;
    public final ParameterType inParamType;
    public final ParameterType outParamType;
    public final String inParamName;
    public final String outParamName;
}
