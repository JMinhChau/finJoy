package com.example.finjoy

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTransactionType(type: Category.TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): Category.TransactionType {
        return Category.TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
}