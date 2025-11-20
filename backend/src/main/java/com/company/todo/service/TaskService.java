package com.company.todo.service;

import com.company.todo.model.Task;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    
    List<Task> getAllTasks();
    
    Task getTaskById(UUID id);
    
    Task createTask(String description);
    
    Task toggleTaskCompletion(UUID id);
    
    void deleteTask(UUID id);
}
