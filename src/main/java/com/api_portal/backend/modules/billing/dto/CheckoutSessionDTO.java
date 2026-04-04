package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionDTO {
    private String sessionId;
    private String checkoutUrl;
    private String planName;
    private String amount;
    private String currency;
}
