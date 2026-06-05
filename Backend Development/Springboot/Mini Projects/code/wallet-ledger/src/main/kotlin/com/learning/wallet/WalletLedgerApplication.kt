package com.learning.wallet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WalletLedgerApplication

fun main(args: Array<String>) {
    runApplication<WalletLedgerApplication>(*args)
}
