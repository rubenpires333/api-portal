package com.api_portal.backend.modules.help.repository;

import com.api_portal.backend.modules.help.entity.HelpCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpCategoryRepository extends JpaRepository<HelpCategory, Long> {
    List<HelpCategory> findByActiveOrderByDisplayOrderAsc(Boolean active);
    List<HelpCategory> findAllByOrderByDisplayOrderAsc();
}
