package com.ganesh.hisabkitabpro.data.repository.converters

import androidx.room.TypeConverter
import com.ganesh.hisabkitabpro.domain.model.TransactionType

class TransactionTypeConverter {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
}