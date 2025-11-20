package com.company.todo.service;

import com.company.todo.exception.TaskNotFoundException;
import com.company.todo.model.Task;
import com.company.todo.repository.TaskRepository;
import com.company.todo.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    
    @Mock
    private TaskRepository taskRepository;
    
    @InjectMocks
    private TaskServiceImpl taskService;
    
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
    void getAllTasks_ShouldReturnTasksSortedByCreatedAtDescending() {
        LocalDateTime now = LocalDateTime.now();
        Task task1 = Task.builder()
                .id(UUID.randomUUID())
                .description("Task 1")
                .createdAt(now.minusHours(2))
                .build();
        
        Task task2 = Task.builder()
                .id(UUID.randomUUID())
                .description("Task 2")
                .createdAt(now.minusHours(1))
                .build();
        
        Task task3 = Task.builder()
                .id(UUID.randomUUID())
                .description("Task 3")
                .createdAt(now)
                .build();
        
        when(taskRepository.findAll()).thenReturn(Arrays.asList(task1, task2, task3));
        
        List<Task> result = taskService.getAllTasks();
        
        assertEquals(3, result.size());
        assertEquals("Task 3", result.get(0).getDescription());
        assertEquals("Task 2", result.get(1).getDescription());
        assertEquals("Task 1", result.get(2).getDescription());
        verify(taskRepository, times(1)).findAll();
    }
    
    @Test
    void getTaskById_ShouldReturnTask_WhenTaskExists() {
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        
        Task result = taskService.getTaskById(testTaskId);
        
        assertNotNull(result);
        assertEquals(testTaskId, result.getId());
        assertEquals("Test task", result.getDescription());
        verify(taskRepository, times(1)).findById(testTaskId);
    }
    
    @Test
    void getTaskById_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(nonExistentId));
        verify(taskRepository, times(1)).findById(nonExistentId);
    }
    
    @Test
    void createTask_ShouldCreateAndReturnNewTask() {
        String description = "New task";
        Task newTask = Task.builder()
                .id(UUID.randomUUID())
                .description(description)
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(taskRepository.save(any(Task.class))).thenReturn(newTask);
        
        Task result = taskService.createTask(description);
        
        assertNotNull(result);
        assertEquals(description, result.getDescription());
        assertFalse(result.isCompleted());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getCompletedAt());
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void toggleTaskCompletion_ShouldMarkTaskAsCompleted_WhenTaskIsIncomplete() {
        testTask.setCompleted(false);
        testTask.setCompletedAt(null);
        
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Task result = taskService.toggleTaskCompletion(testTaskId);
        
        assertTrue(result.isCompleted());
        assertNotNull(result.getCompletedAt());
        verify(taskRepository, times(1)).findById(testTaskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void toggleTaskCompletion_ShouldMarkTaskAsIncomplete_WhenTaskIsCompleted() {
        testTask.setCompleted(true);
        testTask.setCompletedAt(LocalDateTime.now());
        
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Task result = taskService.toggleTaskCompletion(testTaskId);
        
        assertFalse(result.isCompleted());
        assertNull(result.getCompletedAt());
        verify(taskRepository, times(1)).findById(testTaskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void toggleTaskCompletion_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        assertThrows(TaskNotFoundException.class, () -> taskService.toggleTaskCompletion(nonExistentId));
        verify(taskRepository, times(1)).findById(nonExistentId);
        verify(taskRepository, never()).save(any(Task.class));
    }
    
    @Test
    void deleteTask_ShouldDeleteTask_WhenTaskExists() {
        when(taskRepository.existsById(testTaskId)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(testTaskId);
        
        assertDoesNotThrow(() -> taskService.deleteTask(testTaskId));
        
        verify(taskRepository, times(1)).existsById(testTaskId);
        verify(taskRepository, times(1)).deleteById(testTaskId);
    }
    
    @Test
    void deleteTask_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        when(taskRepository.existsById(nonExistentId)).thenReturn(false);
        
        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(nonExistentId));
        verify(taskRepository, times(1)).existsById(nonExistentId);
        verify(taskRepository, never()).deleteById(any(UUID.class));
    }
}
