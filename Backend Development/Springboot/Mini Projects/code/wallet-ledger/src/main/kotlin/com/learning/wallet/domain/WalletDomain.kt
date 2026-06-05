package com.learning.wallet.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class WalletStatus { ACTIVE, FROZEN, CLOSED }
enum class LedgerTransactionStatus { POSTED, REVERSED, FAILED }
enum class LedgerEntryType { DEBIT, CREDIT }

@Entity
@Table(name = "wallets")
class Wallet(
    @Id val id: UUID = UUID.randomUUID(),
    val ownerName: String,
    val currency: String,
    @Enumerated(EnumType.STRING) var status: WalletStatus = WalletStatus.ACTIVE
) {
    fun ensureCanTransact() {
        check(status == WalletStatus.ACTIVE) { "Wallet is not active" }
    }
}

@Entity
@Table(name = "ledger_transactions")
class LedgerTransaction(
    @Id val id: UUID = UUID.randomUUID(),
    val idempotencyKey: String,
    val referenceType: String,
    @Enumerated(EnumType.STRING) val status: LedgerTransactionStatus = LedgerTransactionStatus.POSTED,
    val createdAt: Instant = Instant.now(),
    @OneToMany(mappedBy = "transaction", cascade = [CascadeType.ALL], orphanRemoval = true)
    val entries: MutableList<LedgerEntry> = mutableListOf()
) {
    fun addEntry(wallet: Wallet, type: LedgerEntryType, amount: BigDecimal) {
        entries.add(LedgerEntry(transaction = this, wallet = wallet, entryType = type, amount = amount, currency = wallet.currency))
    }

    fun validateBalanced() {
        val debit = entries.filter { it.entryType == LedgerEntryType.DEBIT }.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }
        val credit = entries.filter { it.entryType == LedgerEntryType.CREDIT }.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }
        check(entries.isNotEmpty()) { "Ledger transaction needs entries" }
        check(debit == credit) { "Ledger transaction is not balanced" }
    }
}

@Entity
@Table(name = "ledger_entries")
class LedgerEntry(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_id") val transaction: LedgerTransaction,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "wallet_id") val wallet: Wallet,
    @Enumerated(EnumType.STRING) val entryType: LedgerEntryType,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: Instant = Instant.now()
)
