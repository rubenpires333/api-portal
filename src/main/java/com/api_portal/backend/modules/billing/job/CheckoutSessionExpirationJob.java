package com.api_portal.backend.modules.billing.job;

import com.api_portal.backend.modules.billing.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job para expirar sessões de checkout antigas (30 minutos)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutSessionExpirationJob {

    private final CheckoutService checkoutService;

    @Scheduled(cron = "0 */5 * * * *") // A cada 5 minutos
    public void expireOldSessions() {
        log.debug("Executando job de expiração de sessões de checkout");
        checkoutService.expireOldSessions();
    }
}
