package com.example.basekotlin.ui.files.video.model

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

// Định nghĩa 4 chế độ Aspect Ratio với resizeMode tương ứng
@UnstableApi
enum class VideoAspectRatio(
    val label: String,
    val ratioString: String?,
    val resizeMode: Int
) {

    // 1. Fit: Vừa màn hình theo tỷ lệ gốc của video (không cắt xén)
    BEST_FIT("Fit", null, AspectRatioFrameLayout.RESIZE_MODE_FIT),
    // 2. Fill: Phóng to để lấp đầy màn hình (có thể bị cắt xén)
    FILL("Fill", null, AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    // 3. 4:3: Khung 4:3 và crop video vừa khít khung
    RATIO_4_3("4:3", "4:3", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    // 4. 16:9: Khung 16:9 và crop video vừa khít khung
    RATIO_16_9("16:9", "16:9", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    // 5. 1:1: Khung vuông 1:1 và crop video vừa khít khung
    RATIO_1_1("1:1", "1:1", AspectRatioFrameLayout.RESIZE_MODE_FIT),
}