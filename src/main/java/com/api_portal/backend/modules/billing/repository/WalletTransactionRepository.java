package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    
    Page<WalletTransaction> findByWalletOrderByCreatedAtDesc(ProviderWallet wallet, Pageable pageable);
    
    @Query("SELECT t FROM WalletTransaction t WHERE t.status = :status AND t.availableAt <= :now")
    List<WalletTransaction> findPendingTransactionsReadyForRelease(TransactionStatus status, LocalDateTime now);
    
    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet = :wallet AND t.referenceId = :referenceId")
    java.util.Optional<WalletTransaction> findByWalletAndReferenceId(ProviderWallet wallet, UUID referenceId);
}
