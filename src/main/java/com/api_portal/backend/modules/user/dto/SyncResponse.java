package com.api_portal.backend.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {
    
    private Integer rolesCreated;
    private Integer rolesUpdated;
    private Integer groupsCreated;
    private Integer groupsUpdated;
    private LocalDateTime syncedAt;
    private String message;
}
