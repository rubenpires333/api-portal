package com.api_portal.backend.modules.user.domain.repository;

import com.api_portal.backend.modules.user.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByUserId(UUID userId);
}
