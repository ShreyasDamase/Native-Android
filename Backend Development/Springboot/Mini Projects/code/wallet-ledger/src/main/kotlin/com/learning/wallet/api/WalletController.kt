package com.learning.wallet.api

import com.learning.wallet.application.WalletService
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class CreateWalletRequest(@field:NotBlank val ownerName: String, @field:NotBlank val currency: String)
data class DepositRequest(@field:DecimalMin("0.01") val amount: BigDecimal)
data class TransferRequest(val fromWalletId: UUID, val toWalletId: UUID, @field:DecimalMin("0.01") val amount: BigDecimal)

@RestController
@RequestMapping("/api/v1")
class WalletController(private val service: WalletService) {
    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    fun createWallet(@Valid @RequestBody request: CreateWalletRequest) =
        service.createWallet(request.ownerName, request.currency)

    @GetMapping("/wallets/{walletId}")
    fun wallet(@PathVariable walletId: UUID) = service.getWallet(walletId)

    @GetMapping("/wallets/{walletId}/balance")
    fun balance(@PathVariable walletId: UUID) = mapOf("walletId" to walletId, "balance" to service.balance(walletId))

    @PostMapping("/wallets/{walletId}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    fun deposit(
        @PathVariable walletId: UUID,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: DepositRequest
    ) = service.deposit(walletId, request.amount, idempotencyKey)

    @PostMapping("/wallet-transfers")
    @ResponseStatus(HttpStatus.CREATED)
    fun transfer(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: TransferRequest
    ) = service.transfer(request.fromWalletId, request.toWalletId, request.amount, idempotencyKey)

    @GetMapping("/ledger/transactions/{transactionId}/entries")
    fun ledger(@PathVariable transactionId: UUID) = service.ledger(transactionId)
}
