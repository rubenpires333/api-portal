package com.api_portal.backend.modules.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApiCategoryResponse {
    
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Integer apiCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
