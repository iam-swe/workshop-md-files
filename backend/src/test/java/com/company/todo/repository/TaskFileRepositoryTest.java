package com.company.todo.repository;

import com.company.todo.exception.FileStorageException;
import com.company.todo.model.Task;
import com.company.todo.repository.impl.TaskFileRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TaskFileRepositoryTest {
    
    @TempDir
    Path tempDir;
    
    private TaskFileRepositoryImpl repository;
    private Path testFilePath;
    
    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("tasks.json");
        repository = new TaskFileRepositoryImpl(testFilePath.toString());
        repository.init();
    }
    
    @Test
    void init_ShouldCreateFileAndDirectory_WhenTheyDoNotExist() {
        assertTrue(Files.exists(testFilePath));
        assertTrue(Files.exists(testFilePath.getParent()));
    }
    
    @Test
    void findAll_ShouldReturnEmptyList_WhenNoTasksExist() {
        List<Task> tasks = repository.findAll();
        
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }
    
    @Test
    void save_ShouldCreateNewTask_WithGeneratedId() {
        Task task = Task.builder()
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        
        assertNotNull(savedTask.getId());
        assertEquals("Test task", savedTask.getDescription());
        assertFalse(savedTask.isCompleted());
    }
    
    @Test
    void save_ShouldUpdateExistingTask_WhenIdExists() {
        Task task = Task.builder()
                .description("Original description")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        UUID taskId = savedTask.getId();
        
        savedTask.setDescription("Updated description");
        savedTask.setCompleted(true);
        Task updatedTask = repository.save(savedTask);
        
        assertEquals(taskId, updatedTask.getId());
        assertEquals("Updated description", updatedTask.getDescription());
        assertTrue(updatedTask.isCompleted());
    }
    
    @Test
    void findById_ShouldReturnTask_WhenTaskExists() {
        Task task = Task.builder()
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        Optional<Task> foundTask = repository.findById(savedTask.getId());
        
        assertTrue(foundTask.isPresent());
        assertEquals(savedTask.getId(), foundTask.get().getId());
        assertEquals("Test task", foundTask.get().getDescription());
    }
    
    @Test
    void findById_ShouldReturnEmpty_WhenTaskDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        
        Optional<Task> foundTask = repository.findById(nonExistentId);
        
        assertFalse(foundTask.isPresent());
    }
    
    @Test
    void deleteById_ShouldRemoveTask_WhenTaskExists() {
        Task task = Task.builder()
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        UUID taskId = savedTask.getId();
        
        repository.deleteById(taskId);
        Optional<Task> foundTask = repository.findById(taskId);
        
        assertFalse(foundTask.isPresent());
    }
    
    @Test
    void existsById_ShouldReturnTrue_WhenTaskExists() {
        Task task = Task.builder()
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        
        assertTrue(repository.existsById(savedTask.getId()));
    }
    
    @Test
    void existsById_ShouldReturnFalse_WhenTaskDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        
        assertFalse(repository.existsById(nonExistentId));
    }
    
    @Test
    void save_ShouldPersistMultipleTasks() {
        Task task1 = Task.builder()
                .description("Task 1")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task task2 = Task.builder()
                .description("Task 2")
                .completed(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        repository.save(task1);
        repository.save(task2);
        
        List<Task> tasks = repository.findAll();
        
        assertEquals(2, tasks.size());
    }
    
    @Test
    void save_ShouldMaintainDataIntegrity_AfterMultipleOperations() {
        Task task = Task.builder()
                .description("Test task")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Task savedTask = repository.save(task);
        
        TaskFileRepositoryImpl newRepositoryInstance = new TaskFileRepositoryImpl(testFilePath.toString());
        newRepositoryInstance.init();
        
        Optional<Task> foundTask = newRepositoryInstance.findById(savedTask.getId());
        
        assertTrue(foundTask.isPresent());
        assertEquals(savedTask.getId(), foundTask.get().getId());
        assertEquals("Test task", foundTask.get().getDescription());
    }
}
