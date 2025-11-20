package com.company.todo.service.impl;

import com.company.todo.exception.TaskNotFoundException;
import com.company.todo.model.Task;
import com.company.todo.repository.TaskRepository;
import com.company.todo.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    
    private final TaskRepository taskRepository;
    
    @Override
    public List<Task> getAllTasks() {
        log.debug("Fetching all tasks");
        List<Task> tasks = taskRepository.findAll();
        
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public Task getTaskById(UUID id) {
        log.debug("Fetching task with ID: {}", id);
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
    }
    
    @Override
    public Task createTask(String description) {
        log.debug("Creating new task with description: {}", description);
        
        Task task = Task.builder()
                .description(description)
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = taskRepository.save(task);
        log.info("Created task with ID: {}", savedTask.getId());
        
        return savedTask;
    }
    
    @Override
    public Task toggleTaskCompletion(UUID id) {
        log.debug("Toggling completion status for task ID: {}", id);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
        
        task.setCompleted(!task.isCompleted());
        
        if (task.isCompleted()) {
            task.setCompletedAt(LocalDateTime.now());
            log.debug("Task marked as completed: {}", id);
        } else {
            task.setCompletedAt(null);
            log.debug("Task marked as incomplete: {}", id);
        }
        
        return taskRepository.save(task);
    }
    
    @Override
    public void deleteTask(UUID id) {
        log.debug("Deleting task with ID: {}", id);
        
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException("Task not found with ID: " + id);
        }
        
        taskRepository.deleteById(id);
        log.info("Deleted task with ID: {}", id);
    }
}
