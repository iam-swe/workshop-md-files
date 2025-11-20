package com.company.todo.controller;

import com.company.todo.dto.request.CreateTaskRequest;
import com.company.todo.exception.TaskNotFoundException;
import com.company.todo.model.Task;
import com.company.todo.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TaskService taskService;
    
    private Task testTask;
    private UUID testTaskId;
    
    @BeforeEach
    void setUp() {
        testTaskId = UUID.randomUUID();
        testTask = Task.builder()
                .id(testTaskId)
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void getAllTasks_ShouldReturnListOfTasks() throws Exception {
        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .description("Task 1")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .description("Task 2")
                .completed(true)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        
        when(taskService.getAllTasks()).thenReturn(Arrays.asList(task1, task2));
        
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description", is("Task 1")))
                .andExpect(jsonPath("$[0].completed", is(false)))
                .andExpect(jsonPath("$[1].description", is("Task 2")))
                .andExpect(jsonPath("$[1].completed", is(true)));
        
        verify(taskService, times(1)).getAllTasks();
    }
    
    @Test
    void getTaskById_ShouldReturnTask_WhenTaskExists() throws Exception {
        when(taskService.getTaskById(testTaskId)).thenReturn(testTask);
        
        mockMvc.perform(get("/api/v1/tasks/{id}", testTaskId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testTaskId.toString())))
                .andExpect(jsonPath("$.description", is("Test task")))
                .andExpect(jsonPath("$.completed", is(false)));
        
        verify(taskService, times(1)).getTaskById(testTaskId);
    }
    
    @Test
    void getTaskById_ShouldReturn404_WhenTaskDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(taskService.getTaskById(nonExistentId))
                .thenThrow(new TaskNotFoundException("Task not found with ID: " + nonExistentId));
        
        mockMvc.perform(get("/api/v1/tasks/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Task not found")));
        
        verify(taskService, times(1)).getTaskById(nonExistentId);
    }
    
    @Test
    void createTask_ShouldReturnCreatedTask_WithValidRequest() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .description("New task")
                .build();
        
        Task newTask = Task.builder()
                .id(UUID.randomUUID())
                .description("New task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(taskService.createTask("New task")).thenReturn(newTask);
        
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("New task")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.id").exists());
        
        verify(taskService, times(1)).createTask("New task");
    }
    
    @Test
    void createTask_ShouldReturn400_WhenDescriptionIsBlank() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .description("")
                .build();
        
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
        
        verify(taskService, never()).createTask(any());
    }
    
    @Test
    void createTask_ShouldReturn400_WhenDescriptionIsTooLong() throws Exception {
        String longDescription = "a".repeat(1001);
        CreateTaskRequest request = CreateTaskRequest.builder()
                .description(longDescription)
                .build();
        
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
        
        verify(taskService, never()).createTask(any());
    }
    
    @Test
    void toggleTaskCompletion_ShouldReturnUpdatedTask() throws Exception {
        Task toggledTask = Task.builder()
                .id(testTaskId)
                .description("Test task")
                .completed(true)
                .createdAt(testTask.getCreatedAt())
                .completedAt(LocalDateTime.now())
                .build();
        
        when(taskService.toggleTaskCompletion(testTaskId)).thenReturn(toggledTask);
        
        mockMvc.perform(patch("/api/v1/tasks/{id}/toggle", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testTaskId.toString())))
                .andExpect(jsonPath("$.completed", is(true)))
                .andExpect(jsonPath("$.completedAt").exists());
        
        verify(taskService, times(1)).toggleTaskCompletion(testTaskId);
    }
    
    @Test
    void toggleTaskCompletion_ShouldReturn404_WhenTaskDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(taskService.toggleTaskCompletion(nonExistentId))
                .thenThrow(new TaskNotFoundException("Task not found with ID: " + nonExistentId));
        
        mockMvc.perform(patch("/api/v1/tasks/{id}/toggle", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
        
        verify(taskService, times(1)).toggleTaskCompletion(nonExistentId);
    }
    
    @Test
    void deleteTask_ShouldReturn204_WhenTaskExists() throws Exception {
        doNothing().when(taskService).deleteTask(testTaskId);
        
        mockMvc.perform(delete("/api/v1/tasks/{id}", testTaskId))
                .andExpect(status().isNoContent());
        
        verify(taskService, times(1)).deleteTask(testTaskId);
    }
    
    @Test
    void deleteTask_ShouldReturn404_WhenTaskDoesNotExist() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new TaskNotFoundException("Task not found with ID: " + nonExistentId))
                .when(taskService).deleteTask(nonExistentId);
        
        mockMvc.perform(delete("/api/v1/tasks/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
        
        verify(taskService, times(1)).deleteTask(nonExistentId);
    }
}
