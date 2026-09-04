package com.example.basekotlin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfToImageConverter {

    /**
     * Chuyển đổi danh sách file PDF thành các file ảnh PNG/JPEG.
     * @param context Context của ứng dụng.
     * @param pdfPaths Danh sách đường dẫn tuyệt đối của các file PDF cần convert.
     * @return Danh sách đường dẫn các file ảnh đã được tạo thành công.
     */
    fun convertPdfListToImages(context: Context, pdfPaths: Set<String>): List<String> {
        val createdImagePaths = mutableListOf<String>()

        // 1. Tạo thư mục lưu trữ ảnh trong Pictures/ShareFile
        val rootDir = Environment.getExternalStorageDirectory()
        val outputFolder = File(rootDir, "ShareFile/PdfConverter/Image")
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        // 2. Duyệt qua từng đường dẫn PDF đã chọn
        for (pdfPath in pdfPaths) {
            val pdfFile = File(pdfPath)
            if (!pdfFile.exists() || !pdfFile.canRead()) {
                continue
            }

            var fileDescriptor: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null

            try {
                // Mở file descriptor chế độ chỉ đọc
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor)

                val pageCount = pdfRenderer.pageCount
                val baseFileName = pdfFile.nameWithoutExtension

                // Duyệt qua từng trang của file PDF
                for (pageIndex in 0 until pageCount) {
                    val page = pdfRenderer.openPage(pageIndex)

                    // Tạo Bitmap tương ứng kích thước của trang
                    val bitmap = Bitmap.createBitmap(
                        page.width,
                        page.height,
                        Bitmap.Config.ARGB_8888
                    )

                    // Tô nền trắng vì trang PDF có thể trong suốt
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    // Render nội dung trang PDF lên bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Tạo tên file ảnh đầu ra (Ví dụ: Document_page_1_1712345678.jpg)
                    val timestamp = System.currentTimeMillis()
                    val imageName = "${baseFileName}_page_${pageIndex + 1}_$timestamp.jpg"
                    val imageFile = File(outputFolder, imageName)

                    // Ghi bitmap ra file JPG
                    val outputStream = FileOutputStream(imageFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    // Giải phóng bộ nhớ bitmap
                    bitmap.recycle()

                    createdImagePaths.add(imageFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Đóng các luồng renderer và descriptor
                try {
                    pdfRenderer?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    fileDescriptor?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Quét media scanner để hệ thống nhận diện các ảnh mới tạo
        if (createdImagePaths.isNotEmpty()) {
            val pathArray = createdImagePaths.toTypedArray()
            val mimeTypeArray = Array(createdImagePaths.size) { "image/jpeg" }
            MediaScannerConnection.scanFile(
                context.applicationContext,
                pathArray,
                mimeTypeArray,
                null
            )
        }

        return createdImagePaths
    }
}
