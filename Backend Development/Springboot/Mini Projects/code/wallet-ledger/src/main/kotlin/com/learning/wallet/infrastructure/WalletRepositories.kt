package com.learning.wallet.infrastructure

import com.learning.wallet.domain.LedgerEntry
import com.learning.wallet.domain.LedgerTransaction
import com.learning.wallet.domain.Wallet
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface WalletRepository : JpaRepository<Wallet, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    fun findWithLockingById(@Param("id") id: UUID): Optional<Wallet>
}

interface LedgerTransactionRepository : JpaRepository<LedgerTransaction, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<LedgerTransaction>
}

interface LedgerEntryRepository : JpaRepository<LedgerEntry, UUID> {
    fun findByWalletId(walletId: UUID): List<LedgerEntry>
    fun findByTransactionId(transactionId: UUID): List<LedgerEntry>
}
