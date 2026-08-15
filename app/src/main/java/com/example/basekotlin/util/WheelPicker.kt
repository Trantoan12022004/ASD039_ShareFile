package com.example.basekotlin.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import androidx.core.content.res.ResourcesCompat
import com.example.basekotlin.R
import kotlin.math.abs
import kotlin.math.roundToInt

class WheelPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue: Int = 0
    var maxValue: Int = 59

    var value: Int = 0
        set(v) {
            field = v.coerceIn(minValue, maxValue)
            scrollOffsetY = (field - minValue) * itemHeight
            invalidate()
        }

    var onValueChanged: ((value: Int) -> Unit)? = null

    // KÍCH THƯỚC CHỮ VÀ MÀU SẮC CHUẨN NHƯ ẢNH
    private val selectedTextSize = spToPx(28f)       // Cỡ chữ số ở giữa (TO)
    private val unselectedTextSize = spToPx(16f)     // Cỡ chữ số trên/dưới (NHỎ)
    private val selectedTextColor = Color.WHITE       // Màu trắng sáng số ở giữa
    private val unselectedTextColor = Color.parseColor("#80FFFFFF") // Màu mờ số trên/dưới
    private val dividerColor = Color.parseColor("#40FFFFFF")       // Màu đường gạch ngang Divider

    private var itemHeight = dpToPx(44f)
    private var scrollOffsetY = 0f

    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = selectedTextSize
        color = selectedTextColor
    }

    private val unselectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = unselectedTextSize
        color = unselectedTextColor
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        strokeWidth = dpToPx(1f)
    }

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private var lastY = 0f

    init {
        try {
            val font = ResourcesCompat.getFont(context, R.font.plusjakartasans_bold)
            if (font != null) {
                selectedPaint.typeface = font
                unselectedPaint.typeface = font
            }
        } catch (_: Exception) {}
    }

    private fun getItemCount(): Int {
        return maxValue - minValue + 1
    }

    // Đưa index về khoảng 0..(count-1) để vòng chọn chạy liên tục 59 → 00 → 01...
    private fun wrapIndex(index: Int): Int {
        val count = getItemCount()
        if (count <= 0) {
            return 0
        }

        var wrapped = index % count
        if (wrapped < 0) {
            wrapped += count
        }
        return wrapped
    }

    // === ĐÃ SỬA KIỂU DỮ LIỆU TẠI ĐÂY: widthMeasureSpec: Int, heightMeasureSpec: Int ===
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(dpToPx(50f).toInt())
        val height = (itemHeight * 3).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val centerY = height / 2f

        // Vẽ 2 đường gạch ngang Divider trên và dưới số ở giữa
        val topDividerY = centerY - itemHeight / 2f
        val bottomDividerY = centerY + itemHeight / 2f
        canvas.drawLine(0f, topDividerY, width.toFloat(), topDividerY, dividerPaint)
        canvas.drawLine(0f, bottomDividerY, width.toFloat(), bottomDividerY, dividerPaint)

        // Index tuyệt đối theo offset (có thể âm / vượt max vì cuộn vòng)
        val currentIndex = kotlin.math.floor((scrollOffsetY / itemHeight).toDouble()).toInt()

        for (i in (currentIndex - 2)..(currentIndex + 2)) {
            val itemValue = minValue + wrapIndex(i)
            val itemY = centerY + (i * itemHeight - scrollOffsetY)
            val formattedText = String.format("%02d", itemValue)
            val distanceToCenter = abs(itemY - centerY)

            if (distanceToCenter < itemHeight / 3f) {
                // SỐ Ở GIỮA -> Cỡ chữ TO
                val fontMetrics = selectedPaint.fontMetrics
                val baseline = itemY - (fontMetrics.top + fontMetrics.bottom) / 2f
                canvas.drawText(formattedText, cx, baseline, selectedPaint)
            } else {
                // SỐ TRÊN/DƯỚI -> Cỡ chữ NHỎ
                val fontMetrics = unselectedPaint.fontMetrics
                val baseline = itemY - (fontMetrics.top + fontMetrics.bottom) / 2f
                canvas.drawText(formattedText, cx, baseline, unselectedPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.forceFinished(true)
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = lastY - event.y
                // Không clamp: cho phép cuộn vượt biên để vòng lại 59 → 00
                scrollOffsetY += dy
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vY = velocityTracker?.yVelocity ?: 0f
                if (abs(vY) > 500) {
                    // Fling không giới hạn biên để quán tính vẫn quay cùng chiều qua 00
                    scroller.fling(
                        0, scrollOffsetY.toInt(),
                        0, -vY.toInt(),
                        0, 0,
                        Int.MIN_VALUE / 4, Int.MAX_VALUE / 4
                    )
                } else {
                    snapToNearest()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollOffsetY = scroller.currY.toFloat()
            invalidate()
            if (scroller.isFinished) {
                snapToNearest()
            }
        }
    }

    private fun snapToNearest() {
        val rawIndex = (scrollOffsetY / itemHeight).roundToInt()
        val wrappedIndex = wrapIndex(rawIndex)
        val newValue = minValue + wrappedIndex

        // Chuẩn hóa offset về khoảng 0..(count-1) để tránh số quá lớn sau nhiều vòng
        scrollOffsetY = wrappedIndex * itemHeight

        if (newValue != value) {
            value = newValue
            onValueChanged?.invoke(value)
        } else {
            invalidate()
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    private fun spToPx(sp: Float): Float = sp * resources.displayMetrics.scaledDensity
}
