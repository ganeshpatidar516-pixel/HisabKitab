package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.domain.ocr.OCRBillProcessor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OcrBillProcessorEntryPoint {
    fun ocrBillProcessor(): OCRBillProcessor
}
