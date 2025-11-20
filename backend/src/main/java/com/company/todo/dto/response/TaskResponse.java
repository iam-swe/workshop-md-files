package com.company.todo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private String id;
    private String description;
    private boolean completed;
    private String createdAt;
    private String completedAt;
}
