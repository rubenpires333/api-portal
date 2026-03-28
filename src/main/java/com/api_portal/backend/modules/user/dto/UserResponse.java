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
public class UserResponse {
    
    private UUID id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String avatarUrl;
    private Boolean emailVerified;
    private Boolean active;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Set<RoleInfo> roles;
    private String bio;
    private String company;
    private String location;
    private String website;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String lastModifiedBy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private UUID id;
        private String name;
        private String code;
        private String description;
    }
}
