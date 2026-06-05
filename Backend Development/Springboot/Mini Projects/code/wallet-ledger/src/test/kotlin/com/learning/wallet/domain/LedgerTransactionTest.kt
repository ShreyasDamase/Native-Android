package com.learning.wallet.domain

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LedgerTransactionTest {
    @Test
    fun `unbalanced ledger transaction is rejected`() {
        val debitWallet = Wallet(ownerName = "A", currency = "INR")
        val creditWallet = Wallet(ownerName = "B", currency = "INR")
        val tx = LedgerTransaction(idempotencyKey = "k1", referenceType = "TEST")
        tx.addEntry(debitWallet, LedgerEntryType.DEBIT, BigDecimal("100.00"))
        tx.addEntry(creditWallet, LedgerEntryType.CREDIT, BigDecimal("90.00"))

        assertThatThrownBy { tx.validateBalanced() }.isInstanceOf(IllegalStateException::class.java)
    }
}
