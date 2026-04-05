package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    
    Page<WithdrawalRequest> findByWalletOrderByRequestedAtDesc(ProviderWallet wallet, Pageable pageable);
    
    List<WithdrawalRequest> findByStatus(WithdrawalStatus status);
    
    Page<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalStatus status, Pageable pageable);
    
    long countByStatus(WithdrawalStatus status);
    
    // Métodos para Platform Wallet
    @Query("SELECT COALESCE(SUM(w.requestedAmount), 0) FROM WithdrawalRequest w WHERE w.status = :status")
    Optional<BigDecimal> sumAmountByStatus(WithdrawalStatus status);
    
    @Query("SELECT COALESCE(SUM(w.feeAmount), 0) FROM WithdrawalRequest w WHERE w.status = :status")
    Optional<BigDecimal> sumFeesByStatus(WithdrawalStatus status);
}
