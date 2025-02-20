package com.example.finjoy

import java.time.LocalDate

sealed class TransactionListItem {
    data class DateHeader(
        val date: LocalDate,
        val totalAmount: Double,
        val transactions: List<TransactionWithCategory>
    ) : TransactionListItem()

    data class TransactionItem(
        val transaction: TransactionWithCategory
    ) : TransactionListItem()
}