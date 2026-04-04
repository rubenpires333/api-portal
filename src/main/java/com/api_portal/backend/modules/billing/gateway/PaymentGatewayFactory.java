package com.api_portal.backend.modules.billing.gateway;

import com.api_portal.backend.modules.billing.repository.GatewayConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PaymentGatewayFactory {

    private final Map<String, PaymentGateway> gateways;
    private final GatewayConfigRepository gatewayConfigRepo;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList, GatewayConfigRepository gatewayConfigRepo) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(
                PaymentGateway::getType,
                Function.identity()
            ));
        this.gatewayConfigRepo = gatewayConfigRepo;
        log.info("Registered payment gateways: {}", gateways.keySet());
    }

    public PaymentGateway get(String type) {
        PaymentGateway gateway = gateways.get(type.toUpperCase());
        if (gateway == null) {
            throw new IllegalArgumentException("Gateway not found: " + type);
        }
        return gateway;
    }

    public PaymentGateway getActive() {
        return gatewayConfigRepo.findByActiveTrue()
            .map(config -> get(config.getGatewayType().name()))
            .orElseThrow(() -> new IllegalStateException("No active payment gateway configured"));
    }
}
