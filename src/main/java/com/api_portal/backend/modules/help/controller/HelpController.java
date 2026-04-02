package com.api_portal.backend.modules.help.controller;

import com.api_portal.backend.modules.help.dto.HelpCategoryDTO;
import com.api_portal.backend.modules.help.dto.HelpFaqDTO;
import com.api_portal.backend.modules.help.service.HelpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;

@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
public class HelpController {
    
    private final HelpService helpService;
    
    // Public endpoints - accessible to all authenticated users
    @GetMapping("/categories")
    public ResponseEntity<List<HelpCategoryDTO>> getActiveCategories() {
        return ResponseEntity.ok(helpService.getActiveCategories());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<HelpFaqDTO>> searchFaqs(@RequestParam String q) {
        return ResponseEntity.ok(helpService.searchFaqs(q));
    }
}
