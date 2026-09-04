package com.example.basekotlin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object ImageToPdfConverter {

    /**
     * Chuyển đổi danh sách đường dẫn ảnh thành một file PDF tổng hợp.
     * Mỗi ảnh sẽ là 1 trang trong file PDF.
     *
     * @param context Context ứng dụng
     * @param imagePaths Danh sách đường dẫn ảnh cần convert
     * @param customFileName Tên file PDF muốn đặt (nếu có)
     * @return Đường dẫn tuyệt đối của file PDF được tạo thành công, hoặc null nếu thất bại
     */
    fun convertImagesToPdf(
        context: Context,
        imagePaths: List<String>,
        customFileName: String? = null
    ): String? {
        if (imagePaths.isEmpty()) {
            return null
        }

        // 1. Tạo thư mục lưu trữ PDF trong ShareFile/PdfConverter/PDF
        val rootDir = Environment.getExternalStorageDirectory()
        val outputFolder = File(rootDir, "ShareFile/PdfConverter/PDF")
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        // 2. Tạo tên file PDF đầu ra
        val timestamp = System.currentTimeMillis()
        val pdfFileName: String
        if (customFileName != null && customFileName.isNotBlank()) {
            pdfFileName = "${customFileName}_$timestamp.pdf"
        } else {
            pdfFileName = "IMG_TO_PDF_$timestamp.pdf"
        }
        val outputFile = File(outputFolder, pdfFileName)

        val pdfDocument = PdfDocument()
        val paint = Paint()
        var pageNumber = 1
        var hasValidPage = false

        try {
            // 3. Duyệt qua từng ảnh và thêm vào làm một trang PDF
            for (imagePath in imagePaths) {
                val imageFile = File(imagePath)
                if (!imageFile.exists() || !imageFile.canRead()) {
                    continue
                }

                // Giải mã file ảnh thành Bitmap
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap == null) {
                    continue
                }

                val imageWidth = bitmap.width
                val imageHeight = bitmap.height

                // Tạo thông tin trang PDF theo kích thước ảnh
                val pageInfo = PdfDocument.PageInfo.Builder(imageWidth, imageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Vẽ nền trắng phòng trường hợp ảnh PNG trong suốt
                canvas.drawColor(Color.WHITE)

                // Vẽ bitmap lên toàn bộ trang
                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                // Hoàn tất trang
                pdfDocument.finishPage(page)
                bitmap.recycle()

                pageNumber = pageNumber + 1
                hasValidPage = true
            }

            if (!hasValidPage) {
                pdfDocument.close()
                return null
            }

            // 4. Ghi file PDF ra bộ nhớ
            val outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // 5. Quét media scanner để hệ thống nhận diện file PDF mới tạo
            val pathArray = arrayOf(outputFile.absolutePath)
            val mimeTypeArray = arrayOf("application/pdf")
            MediaScannerConnection.scanFile(
                context.applicationContext,
                pathArray,
                mimeTypeArray,
                null
            )

            return outputFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                pdfDocument.close()
            } catch (closeEx: Exception) {
                closeEx.printStackTrace()
            }
            return null
        }
    }
}
