package com.api_portal.backend.modules.help.repository;

import com.api_portal.backend.modules.help.entity.HelpFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HelpFaqRepository extends JpaRepository<HelpFaq, UUID> {
    List<HelpFaq> findByCategoryIdAndActiveOrderByDisplayOrderAsc(UUID categoryId, Boolean active);
    List<HelpFaq> findByCategoryIdOrderByDisplayOrderAsc(UUID categoryId);
    
    @Query("SELECT f FROM HelpFaq f WHERE f.active = true AND " +
           "(LOWER(f.question) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.answer) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<HelpFaq> searchFaqs(@Param("search") String search);
}
