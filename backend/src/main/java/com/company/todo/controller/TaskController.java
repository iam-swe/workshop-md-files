package com.company.todo.controller;

import com.company.todo.dto.request.CreateTaskRequest;
import com.company.todo.dto.response.TaskResponse;
import com.company.todo.model.Task;
import com.company.todo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TaskController {
    
    private final TaskService taskService;
    
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        log.debug("GET /api/v1/tasks - Fetching all tasks");
        
        List<Task> tasks = taskService.getAllTasks();
        List<TaskResponse> response = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID id) {
        log.debug("GET /api/v1/tasks/{} - Fetching task", id);
        
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(convertToResponse(task));
    }
    
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        log.debug("POST /api/v1/tasks - Creating new task");
        
        Task task = taskService.createTask(request.getDescription());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertToResponse(task));
    }
    
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TaskResponse> toggleTaskCompletion(@PathVariable UUID id) {
        log.debug("PATCH /api/v1/tasks/{}/toggle - Toggling task completion", id);
        
        Task task = taskService.toggleTaskCompletion(id);
        return ResponseEntity.ok(convertToResponse(task));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/tasks/{} - Deleting task", id);
        
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
    
    private TaskResponse convertToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId().toString())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                .build();
    }
}
