package com.api_portal.backend.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String resource;
    private String action;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
