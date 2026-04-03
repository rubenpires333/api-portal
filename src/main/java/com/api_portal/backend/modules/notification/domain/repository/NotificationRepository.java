package com.api_portal.backend.modules.notification.domain.repository;

import com.api_portal.backend.modules.notification.domain.entity.Notification;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, NotificationType type, Pageable pageable);
    
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, Boolean isRead, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchByUserIdAndTitleOrMessage(@Param("userId") String userId, 
                                                        @Param("search") String search, 
                                                        Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.type = :type " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchByUserIdAndTypeAndTitleOrMessage(@Param("userId") String userId,
                                                               @Param("type") NotificationType type,
                                                               @Param("search") String search,
                                                               Pageable pageable);
    
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "ORDER BY n.isRead ASC, n.createdAt DESC")
    List<Notification> findTop4ByUserIdOrderByIsReadAndCreatedAt(@Param("userId") String userId, Pageable pageable);
    
    long countByUserIdAndIsRead(String userId, Boolean isRead);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId);
    
    void deleteByUserId(String userId);
}
