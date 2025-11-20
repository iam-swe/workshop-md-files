package com.company.todo.repository.impl;

import com.company.todo.exception.FileStorageException;
import com.company.todo.model.Task;
import com.company.todo.repository.TaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TaskFileRepositoryImpl implements TaskRepository {
    
    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public TaskFileRepositoryImpl(@Value("${todo.file.path}") String filePathString) {
        this.filePath = Path.of(filePathString);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
                log.info("Created directory: {}", filePath.getParent());
            }
            
            if (!Files.exists(filePath)) {
                writeTasksToFile(new ArrayList<>());
                log.info("Created empty tasks file: {}", filePath);
            }
            
            if (!Files.isWritable(filePath.getParent())) {
                throw new FileStorageException("Directory is not writable: " + filePath.getParent());
            }
            
            log.info("TaskFileRepository initialized successfully with file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to initialize task file repository", e);
            throw new FileStorageException("Failed to initialize task file repository", e);
        }
    }
    
    @Override
    public List<Task> findAll() {
        lock.readLock().lock();
        try {
            return readTasksFromFile();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Optional<Task> findById(UUID id) {
        lock.readLock().lock();
        try {
            return readTasksFromFile().stream()
                    .filter(task -> task.getId().equals(id))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Task save(Task task) {
        lock.writeLock().lock();
        try {
            List<Task> tasks = readTasksFromFile();
            
            if (task.getId() == null) {
                task.setId(UUID.randomUUID());
                tasks.add(task);
                log.debug("Creating new task with ID: {}", task.getId());
            } else {
                tasks = tasks.stream()
                        .map(t -> t.getId().equals(task.getId()) ? task : t)
                        .collect(Collectors.toList());
                log.debug("Updating task with ID: {}", task.getId());
            }
            
            writeTasksToFile(tasks);
            return task;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void deleteById(UUID id) {
        lock.writeLock().lock();
        try {
            List<Task> tasks = readTasksFromFile();
            List<Task> filteredTasks = tasks.stream()
                    .filter(task -> !task.getId().equals(id))
                    .collect(Collectors.toList());
            
            writeTasksToFile(filteredTasks);
            log.debug("Deleted task with ID: {}", id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean existsById(UUID id) {
        lock.readLock().lock();
        try {
            return readTasksFromFile().stream()
                    .anyMatch(task -> task.getId().equals(id));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private List<Task> readTasksFromFile() {
        try {
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                return new ArrayList<>();
            }
            
            String content = Files.readString(filePath);
            return objectMapper.readValue(content, new TypeReference<List<Task>>() {});
        } catch (IOException e) {
            log.error("Failed to read tasks from file: {}", filePath, e);
            throw new FileStorageException("Failed to read tasks from file", e);
        }
    }
    
    private void writeTasksToFile(List<Task> tasks) {
        try {
            Path tempFile = Files.createTempFile(filePath.getParent(), "tasks", ".tmp");
            
            String jsonContent = objectMapper.writeValueAsString(tasks);
            Files.writeString(tempFile, jsonContent);
            
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            log.debug("Successfully wrote {} tasks to file", tasks.size());
        } catch (IOException e) {
            log.error("Failed to write tasks to file: {}", filePath, e);
            throw new FileStorageException("Failed to write tasks to file", e);
        }
    }
}
