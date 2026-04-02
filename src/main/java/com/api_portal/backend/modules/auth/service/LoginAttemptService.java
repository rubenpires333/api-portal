package com.api_portal.backend.modules.auth.service;

import com.api_portal.backend.modules.auth.domain.LoginAttempt;
import com.api_portal.backend.modules.auth.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    
    private final LoginAttemptRepository loginAttemptRepository;
    
    private static final int MAX_ATTEMPTS_BEFORE_CAPTCHA = 3;
    private static final int MAX_ATTEMPTS_BEFORE_BLOCK = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;
    private static final int RESET_ATTEMPTS_HOURS = 24;
    
    /**
     * Verifica se o usuário precisa de CAPTCHA
     */
    public boolean requiresCaptcha(String email, String ipAddress) {
        return loginAttemptRepository.findByEmailAndIpAddress(email, ipAddress)
            .map(attempt -> {
                // Se estiver bloqueado, também requer captcha
                if (attempt.getBlockedUntil() != null && 
                    attempt.getBlockedUntil().isAfter(LocalDateTime.now())) {
                    return true;
                }
                return attempt.getRequiresCaptcha();
            })
            .orElse(false);
    }
    
    /**
     * Verifica se o usuário está bloqueado
     */
    public boolean isBlocked(String email, String ipAddress) {
        return loginAttemptRepository.findByEmailAndIpAddress(email, ipAddress)
            .map(attempt -> {
                if (attempt.getBlockedUntil() != null && 
                    attempt.getBlockedUntil().isAfter(LocalDateTime.now())) {
                    return true;
                }
                // Se o bloqueio expirou, limpar
                if (attempt.getBlockedUntil() != null && 
                    attempt.getBlockedUntil().isBefore(LocalDateTime.now())) {
                    attempt.setBlockedUntil(null);
                    attempt.setAttempts(0);
                    attempt.setRequiresCaptcha(false);
                    loginAttemptRepository.save(attempt);
                }
                return false;
            })
            .orElse(false);
    }
    
    /**
     * Retorna o tempo restante de bloqueio em minutos
     */
    public long getBlockedMinutesRemaining(String email, String ipAddress) {
        return loginAttemptRepository.findByEmailAndIpAddress(email, ipAddress)
            .map(attempt -> {
                if (attempt.getBlockedUntil() != null && 
                    attempt.getBlockedUntil().isAfter(LocalDateTime.now())) {
                    return java.time.Duration.between(
                        LocalDateTime.now(), 
                        attempt.getBlockedUntil()
                    ).toMinutes();
                }
                return 0L;
            })
            .orElse(0L);
    }
    
    /**
     * Registra uma tentativa de login falhada
     */
    @Transactional
    public void recordFailedAttempt(String email, String ipAddress) {
        LoginAttempt attempt = loginAttemptRepository
            .findByEmailAndIpAddress(email, ipAddress)
            .orElse(LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .attempts(0)
                .requiresCaptcha(false)
                .lastAttempt(LocalDateTime.now())
                .build());
        
        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastAttempt(LocalDateTime.now());
        
        // Ativar CAPTCHA após 3 tentativas
        if (attempt.getAttempts() >= MAX_ATTEMPTS_BEFORE_CAPTCHA) {
            attempt.setRequiresCaptcha(true);
            log.warn("CAPTCHA ativado para {} (IP: {}) após {} tentativas", 
                email, ipAddress, attempt.getAttempts());
        }
        
        // Bloquear após 5 tentativas
        if (attempt.getAttempts() >= MAX_ATTEMPTS_BEFORE_BLOCK) {
            attempt.setBlockedUntil(LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES));
            log.warn("Usuário {} (IP: {}) bloqueado por {} minutos após {} tentativas", 
                email, ipAddress, BLOCK_DURATION_MINUTES, attempt.getAttempts());
        }
        
        loginAttemptRepository.save(attempt);
    }
    
    /**
     * Limpa as tentativas após login bem-sucedido
     */
    @Transactional
    public void resetAttempts(String email, String ipAddress) {
        loginAttemptRepository.findByEmailAndIpAddress(email, ipAddress)
            .ifPresent(attempt -> {
                log.info("Resetando tentativas de login para {} (IP: {})", email, ipAddress);
                loginAttemptRepository.delete(attempt);
            });
    }
    
    /**
     * Limpa tentativas antigas (executado diariamente)
     */
    @Scheduled(cron = "0 0 2 * * *") // 2h da manhã todos os dias
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(RESET_ATTEMPTS_HOURS);
        loginAttemptRepository.deleteOldAttempts(cutoffTime);
        log.info("Limpeza de tentativas de login antigas concluída");
    }
}
