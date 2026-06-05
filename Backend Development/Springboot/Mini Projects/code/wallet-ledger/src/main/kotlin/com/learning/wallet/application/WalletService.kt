package com.learning.wallet.application

import com.learning.wallet.common.ConflictException
import com.learning.wallet.common.NotFoundException
import com.learning.wallet.domain.LedgerEntryType
import com.learning.wallet.domain.LedgerTransaction
import com.learning.wallet.domain.Wallet
import com.learning.wallet.infrastructure.LedgerEntryRepository
import com.learning.wallet.infrastructure.LedgerTransactionRepository
import com.learning.wallet.infrastructure.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

data class WalletView(val walletId: UUID, val ownerName: String, val currency: String, val balance: BigDecimal)
data class LedgerTransactionView(val transactionId: UUID, val entries: List<LedgerEntryView>)
data class LedgerEntryView(val walletId: UUID, val type: LedgerEntryType, val amount: BigDecimal, val currency: String)

@Service
class WalletService(
    private val wallets: WalletRepository,
    private val transactions: LedgerTransactionRepository,
    private val entries: LedgerEntryRepository
) {
    @Transactional
    fun createWallet(ownerName: String, currency: String): WalletView {
        val wallet = wallets.save(Wallet(ownerName = ownerName, currency = currency.uppercase()))
        return wallet.toView(balance(wallet.id))
    }

    @Transactional(readOnly = true)
    fun getWallet(walletId: UUID): WalletView {
        val wallet = wallets.findById(walletId).orElseThrow { NotFoundException("Wallet not found") }
        return wallet.toView(balance(wallet.id))
    }

    @Transactional
    fun deposit(walletId: UUID, amount: BigDecimal, idempotencyKey: String): LedgerTransactionView {
        val existing = transactions.findByIdempotencyKey(idempotencyKey)
        if (existing.isPresent) return existing.get().toView()
        requirePositive(amount)
        val wallet = wallets.findWithLockingById(walletId).orElseThrow { NotFoundException("Wallet not found") }
        wallet.ensureCanTransact()

        val systemWallet = wallets.save(Wallet(ownerName = "SYSTEM_CASH", currency = wallet.currency))
        val tx = LedgerTransaction(idempotencyKey = idempotencyKey, referenceType = "DEPOSIT")
        tx.addEntry(systemWallet, LedgerEntryType.DEBIT, amount)
        tx.addEntry(wallet, LedgerEntryType.CREDIT, amount)
        tx.validateBalanced()
        return transactions.save(tx).toView()
    }

    @Transactional
    fun transfer(fromWalletId: UUID, toWalletId: UUID, amount: BigDecimal, idempotencyKey: String): LedgerTransactionView {
        val existing = transactions.findByIdempotencyKey(idempotencyKey)
        if (existing.isPresent) return existing.get().toView()
        requirePositive(amount)
        if (fromWalletId == toWalletId) throw ConflictException("Cannot transfer to same wallet")

        val from = wallets.findWithLockingById(fromWalletId).orElseThrow { NotFoundException("Sender wallet not found") }
        val to = wallets.findWithLockingById(toWalletId).orElseThrow { NotFoundException("Receiver wallet not found") }
        from.ensureCanTransact()
        to.ensureCanTransact()
        if (from.currency != to.currency) throw ConflictException("Currency mismatch")
        if (balance(from.id) < amount) throw ConflictException("Insufficient balance")

        val tx = LedgerTransaction(idempotencyKey = idempotencyKey, referenceType = "TRANSFER")
        tx.addEntry(from, LedgerEntryType.DEBIT, amount)
        tx.addEntry(to, LedgerEntryType.CREDIT, amount)
        tx.validateBalanced()
        return transactions.save(tx).toView()
    }

    @Transactional(readOnly = true)
    fun balance(walletId: UUID): BigDecimal {
        return entries.findByWalletId(walletId).fold(BigDecimal.ZERO) { acc, entry ->
            when (entry.entryType) {
                LedgerEntryType.CREDIT -> acc + entry.amount
                LedgerEntryType.DEBIT -> acc - entry.amount
            }
        }
    }

    @Transactional(readOnly = true)
    fun ledger(transactionId: UUID): List<LedgerEntryView> =
        entries.findByTransactionId(transactionId).map { LedgerEntryView(it.wallet.id, it.entryType, it.amount, it.currency) }

    private fun requirePositive(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) throw ConflictException("Amount must be positive")
    }
}

fun Wallet.toView(balance: BigDecimal) = WalletView(id, ownerName, currency, balance)

fun LedgerTransaction.toView() = LedgerTransactionView(
    transactionId = id,
    entries = entries.map { LedgerEntryView(it.wallet.id, it.entryType, it.amount, it.currency) }
)
