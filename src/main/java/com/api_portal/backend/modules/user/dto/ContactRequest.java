package com.api_portal.backend.modules.user.dto;

import com.api_portal.backend.modules.user.domain.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {
    private Contact.ContactType type;
    private String value;
    private Boolean isPrimary;
}
