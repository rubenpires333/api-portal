package com.api_portal.backend.modules.settings.repository;

import com.api_portal.backend.modules.settings.domain.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, UUID> {
    
    Optional<PlatformSetting> findByKey(String key);
    
    List<PlatformSetting> findByCategory(String category);
    
    List<PlatformSetting> findByIsPublicTrue();
}
