package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFaqDTO {
    private UUID id;
    private String question;
    private String answer;
    private Integer displayOrder;
    private Boolean active;
}
