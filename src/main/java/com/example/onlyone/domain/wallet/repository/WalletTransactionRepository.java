package com.example.onlyone.domain.wallet.repository;

import com.example.onlyone.domain.wallet.dto.response.UserWalletTransactionDto;
import com.example.onlyone.domain.wallet.entity.Type;
import com.example.onlyone.domain.wallet.entity.Wallet;
import com.example.onlyone.domain.wallet.entity.WalletTransaction;
import com.example.onlyone.domain.wallet.entity.WalletTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletAndTypeAndWalletTransactionStatus(
            Wallet wallet,
            Type type,
            WalletTransactionStatus walletTransactionStatus,
            Pageable pageable
    );
    Page<WalletTransaction> findByWalletAndTypeNotAndWalletTransactionStatus(
            Wallet wallet,
            Type type,
            WalletTransactionStatus walletTransactionStatus,
            Pageable pageable
    );
    Page<WalletTransaction> findByWalletAndWalletTransactionStatus(
            Wallet wallet,
            WalletTransactionStatus walletTransactionStatus,
            Pageable pageable
    );

    @Query("select wt.operationId from WalletTransaction wt where wt.operationId in :operationIds")
    Set<String> findExistingOperationIds(@Param("operationIds") Collection<String> operationIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WalletTransaction w SET w.walletTransactionStatus = :status WHERE w.operationId IN :operationIds AND w.walletTransactionStatus = 'FAILED'")
    int updateFailedToCompleted(@Param("operationIds") Set<String> operationIds,
                                @Param("status") WalletTransactionStatus status);

}