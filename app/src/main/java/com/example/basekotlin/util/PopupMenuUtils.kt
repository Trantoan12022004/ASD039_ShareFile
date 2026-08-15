package com.example.basekotlin.util

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.viewbinding.ViewBinding
import com.example.basekotlin.R

/**
 * Utils dùng chung để hiển thị một popup menu với layout tuỳ biến (custom ViewBinding),
 * neo theo một anchor view (nút "more"...), tự động lật lên/xuống theo khoảng trống còn lại
 * và có animation mượt giống popup menu mặc định của Android.
 *
 * Popup được tạo với "focusable = false" thay vì "true" như PopupWindow mặc định.
 * Lý do: khi popup focusable, Android sẽ coi popup là một Window mới giành focus,
 * việc này khiến hệ thống reset lại system UI flags và làm navigation bar đang bị ẩn
 * hiện trở lại. Popup không focusable vẫn nhận được sự kiện click bên trong bình thường
 * và vẫn tự đóng khi chạm ra ngoài nhờ "isOutsideTouchable = true".
 */
object PopupMenuUtils {

    fun <VB : ViewBinding> showAnchoredMenu(
        anchor: View,
        inflateBinding: (LayoutInflater) -> VB,
        widthRatio: Float = 0.4f,
        marginDp: Float = 16f,
        gapDp: Float = 4f,
        alignEndWithScreen: Boolean = false,
        setupContent: (binding: VB, popupWindow: PopupWindow) -> Unit = { _, _ -> },
    ): PopupWindow {
        // Bước 1: inflate layout custom của popup thông qua ViewBinding do caller cung cấp
        val context = anchor.context
        val inflater = LayoutInflater.from(context)
        val popupBinding = inflateBinding(inflater)

        // Bước 2: tính các kích thước cơ bản theo mật độ màn hình
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val popupWidth = (screenWidth * widthRatio).toInt()
        val margin = (marginDp * density).toInt()
        val gap = (gapDp * density).toInt()

        // Bước 3: đo trước chiều cao popup (wrap_content) để tính được vị trí hiện phù hợp
        val widthSpec = View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        popupBinding.root.measure(widthSpec, heightSpec)
        val popupHeight = popupBinding.root.measuredHeight

        // Bước 4: lấy toạ độ của anchor trên màn hình
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]
        val anchorY = anchorLocation[1]
        val anchorWidth = anchor.width
        val anchorHeight = anchor.height

        // Bước 5: lấy vùng hiển thị thực tế (trừ status bar, nav bar...)
        val visibleFrame = Rect()
        anchor.getWindowVisibleDisplayFrame(visibleFrame)

        // Bước 6: tính vị trí X mong muốn của popup
        var popupX: Int
        if (alignEndWithScreen) {
            // Luôn neo cạnh phải popup theo lề màn hình (dùng cho toolbar cố định)
            popupX = visibleFrame.right - margin - popupWidth
        } else {
            // Căn cạnh phải popup trùng với cạnh phải của anchor
            popupX = anchorX + anchorWidth - popupWidth
        }

        // Bước 7: kẹp popupX trong vùng hiển thị an toàn, tránh tràn màn hình
        val minX = visibleFrame.left + margin
        val maxX = visibleFrame.right - margin - popupWidth
        if (popupX > maxX) {
            popupX = maxX
        }
        if (popupX < minX) {
            popupX = minX
        }
        val xOffset = popupX - anchorX

        // Bước 8: so sánh khoảng trống phía trên/dưới anchor để quyết định hướng hiện popup
        val spaceBelow = visibleFrame.bottom - (anchorY + anchorHeight)
        val spaceAbove = anchorY - visibleFrame.top

        val showBelow: Boolean
        if (spaceBelow >= popupHeight + gap) {
            showBelow = true
        } else if (spaceAbove >= popupHeight + gap) {
            showBelow = false
        } else {
            if (spaceBelow >= spaceAbove) {
                showBelow = true
            } else {
                showBelow = false
            }
        }

        // Bước 9: tính offset Y tương ứng với hướng hiện đã chọn
        val yOffset: Int
        if (showBelow) {
            yOffset = gap
        } else {
            yOffset = -(anchorHeight + popupHeight + gap)
        }

        // Bước 10: khởi tạo PopupWindow với focusable = false để không làm mất focus Activity
        val popupWindow = PopupWindow(
            popupBinding.root,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )

        // Bước 11: cho phép chạm ra ngoài để tự đóng popup, cần có background khác null
        // để hệ thống nhận diện đúng vùng chạm và hiển thị đổ bóng elevation
        popupWindow.isTouchable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 8f * density

        // Bước 12: chọn animation mượt theo đúng hướng hiện, giống hiệu ứng popup menu mặc định
        if (showBelow) {
            popupWindow.animationStyle = R.style.PopupMenuAnimationTop
        } else {
            popupWindow.animationStyle = R.style.PopupMenuAnimationBottom
        }

        // Bước 13: cho caller cấu hình nội dung/sự kiện click bên trong popup trước khi hiện
        setupContent(popupBinding, popupWindow)

        // Bước 14: hiện popup tại vị trí đã tính
        popupWindow.showAsDropDown(anchor, xOffset, yOffset)

        return popupWindow
    }
}
