package com.ganesh.hisabkitabpro.data.repository.converters

import androidx.room.TypeConverter
import com.ganesh.hisabkitabpro.domain.model.SupplierTransactionType

class SupplierTransactionTypeConverter {
    @TypeConverter
    fun fromType(value: SupplierTransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toType(value: String): SupplierTransactionType {
        return SupplierTransactionType.valueOf(value)
    }
}
