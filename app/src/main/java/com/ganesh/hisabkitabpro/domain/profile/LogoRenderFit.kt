package com.ganesh.hisabkitabpro.domain.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Distortion-free logo drawing: scales bitmap to fit inside [dst] preserving aspect ratio,
 * centered (letterboxed). Shared by PDF and salary slip headers.
 */
object LogoRenderFit {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun drawWithinRect(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val srcR = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstR = dst.width() / dst.height()
        val drawRect = RectF(dst)
        if (srcR > dstR) {
            val newHeight = dst.width() / srcR
            val pad = (dst.height() - newHeight) / 2f
            drawRect.top += pad
            drawRect.bottom -= pad
        } else {
            val newWidth = dst.height() * srcR
            val pad = (dst.width() - newWidth) / 2f
            drawRect.left += pad
            drawRect.right -= pad
        }
        canvas.drawBitmap(bitmap, null, drawRect, paint)
    }
}
