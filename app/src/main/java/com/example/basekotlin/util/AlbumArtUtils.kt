package com.example.basekotlin.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size

object AlbumArtUtils {

    private const val TAG = "AlbumArtUtils"

    fun loadAlbumArt(
        context: Context,
        contentUri: Uri,
        albumId: Long,
        size: Size = Size(200, 200)
    ): Bitmap? {

        // Bước 1: ưu tiên lấy art từ bài hát
        if (contentUri != Uri.EMPTY) {
            try {
                return context.contentResolver.loadThumbnail(
                    contentUri,
                    size,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Load track thumbnail failed", e)
            }
        }

        // Bước 2: nếu không có art bài hát thì lấy art album
        if (albumId > 0L) {
            val albumUri = ContentUris.withAppendedId(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumId
            )

            try {
                return context.contentResolver.loadThumbnail(
                    albumUri,
                    size,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Load album thumbnail failed", e)
            }
        }

        // Bước 3: không có ảnh
        return null
    }

    // Cắt ảnh về hình vuông ở giữa rồi bo tròn thành hình tròn — dùng cho mâm đĩa vinyl
    // để ảnh bìa album trông giống nhãn đĩa thật (không còn góc vuông).
    fun getCircularBitmap(sourceBitmap: Bitmap): Bitmap {
        // Bước 1: xác định đường kính là cạnh nhỏ hơn giữa width/height
        var diameter = sourceBitmap.width
        if (sourceBitmap.height < diameter) {
            diameter = sourceBitmap.height
        }

        // Bước 2: tính vùng cắt hình vuông ở chính giữa ảnh gốc
        val cropLeft = (sourceBitmap.width - diameter) / 2
        val cropTop = (sourceBitmap.height - diameter) / 2
        val sourceRect = Rect(cropLeft, cropTop, cropLeft + diameter, cropTop + diameter)
        val destRect = Rect(0, 0, diameter, diameter)

        // Bước 3: tạo bitmap đích trong suốt, vẽ hình tròn rồi ghép ảnh vào bằng SRC_IN
        val outputBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        val circlePaint = Paint()
        circlePaint.isAntiAlias = true
        val radius = diameter / 2f
        canvas.drawCircle(radius, radius, radius, circlePaint)

        circlePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(sourceBitmap, sourceRect, destRect, circlePaint)

        return outputBitmap
    }
}