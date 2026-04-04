package com.api_portal.backend.modules.billing.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSession {
    private String sessionId;
    private String checkoutUrl;
}
