package com.api_portal.backend.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    
    private UUID id;
    private String name;
    private String code;
    private String description;
    private Boolean isSystem;
    private Boolean active;
    private Set<PermissionInfo> permissions;
    private Integer userCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionInfo {
        private UUID id;
        private String name;
        private String code;
        private String resource;
        private String action;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private Boolean active;
        private LocalDateTime lastLogin;
    }
}
