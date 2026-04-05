package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentDTO {
    private String clientSecret;
    private String paymentIntentId;
    private String planName;
    private String amount;
    private String currency;
    private String publishableKey;
}
