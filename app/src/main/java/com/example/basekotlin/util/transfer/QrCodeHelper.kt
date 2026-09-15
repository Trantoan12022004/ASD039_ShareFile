package com.example.basekotlin.util.transfer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Helper tạo và parse QR code cho kết nối transfer
 *
 * QR code chứa JSON: {"ssid":"xxx","password":"xxx","ip":"192.168.x.x","port":8888,"deviceName":"Phone"}
 * Receiver generate QR → Sender scan → parse → kết nối
 */
object QrCodeHelper {

    /**
     * Tạo QR code bitmap từ ConnectionInfo
     *
     * @param info thông tin kết nối cần encode
     * @param size kích thước bitmap (pixel), mặc định 512
     * @return Bitmap chứa QR code
     */
    fun generateQrBitmap(info: ConnectionInfo, size: Int = 512): Bitmap {
        val content = info.toJson()

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1 // margin nhỏ để QR to hơn
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Parse nội dung QR code thành ConnectionInfo
     *
     * @param qrContent chuỗi JSON từ QR scan
     * @return ConnectionInfo nếu parse thành công, null nếu không hợp lệ
     */
    fun parseQrContent(qrContent: String): ConnectionInfo? {
        return ConnectionInfo.fromJson(qrContent)
    }
}
