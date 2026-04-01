package com.api_portal.backend.modules.user.dto;

import com.api_portal.backend.modules.user.domain.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private UUID id;
    private Contact.ContactType type;
    private String value;
    private Boolean isPrimary;
    private Boolean isVerified;
}
