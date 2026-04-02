package com.api_portal.backend.modules.help.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelpCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean active;
    private List<HelpFaqDTO> faqs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
