package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    
    Page<WithdrawalRequest> findByWalletOrderByRequestedAtDesc(ProviderWallet wallet, Pageable pageable);
    
    List<WithdrawalRequest> findByStatus(WithdrawalStatus status);
    
    Page<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalStatus status, Pageable pageable);
}
