package com.example.basekotlin.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.example.basekotlin.R

/**
 * Utils dùng chung để hiển thị 1 dialog dạng "bottom sheet": trượt lên từ đáy màn hình,
 * chiếm toàn bộ chiều ngang, có lớp phủ mờ phía sau — khác với PopupMenuUtils (popup nhỏ
 * neo theo 1 anchor view). Dùng cho các menu hành động quan trọng như "More" trong chế độ
 * chọn nhiều, nơi cần vùng chạm rộng rãi hơn thay vì 1 popup nhỏ.
 */
object BottomDialogUtils {

    fun <VB : ViewBinding> showBottomMenu(
        context: Context,
        inflateBinding: (LayoutInflater) -> VB,
        dimAmount: Float = 0.4f,
        setupContent: (binding: VB, dialog: Dialog) -> Unit = { _, _ -> },
    ): Dialog {
        // Bước 1: tạo Dialog với theme BottomDialog (nền trong suốt, không bo tròn floating)
        val dialog = Dialog(context, R.style.BottomDialog)
        val inflater = LayoutInflater.from(context)
        val binding = inflateBinding(inflater)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // Bước 2: canh dialog xuống đáy màn hình, rộng full ngang, cao theo nội dung
        val window = dialog.window
        if (window != null) {
            window.setGravity(Gravity.BOTTOM)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(dimAmount)
        }

        // Bước 3: cho caller cấu hình nội dung/sự kiện click bên trong dialog trước khi hiện
        setupContent(binding, dialog)

        dialog.show()
        return dialog
    }
}
