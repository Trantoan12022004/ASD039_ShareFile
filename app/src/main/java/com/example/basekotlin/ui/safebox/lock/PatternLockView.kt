package com.example.basekotlin.ui.safebox.lock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt
import com.example.basekotlin.R

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class PatternViewState {
        NORMAL,  // Trạng thái bình thường hoặc đang vẽ (màu xanh lá)
        ERROR,   // Trạng thái vẽ sai (màu đỏ)
        SUCCESS  // Trạng thái mở khóa thành công (màu xanh lá)
    }

    interface OnPatternListener {
        fun onPatternStarted() {}
        fun onPatternProgress(pattern: List<Int>) {}
        fun onPatternCompleted(pattern: List<Int>)
        fun onPatternCleared() {}
    }

    private var patternListener: OnPatternListener? = null

    // Danh sách 9 điểm: index từ 0 đến 8
    private val selectedNodes = mutableListOf<Int>()
    private var viewState = PatternViewState.NORMAL
    private var isInputEnabled = true
    private var isDrawing = false

    private var currentTouchX = 0f
    private var currentTouchY = 0f

    // Khởi tạo và cache các Drawable từ resource
    private val drawableNormalDot = ContextCompat.getDrawable(context, R.drawable.ic_pattern_dot_normal)
    private val drawableActiveDot = ContextCompat.getDrawable(context, R.drawable.ic_pattern_dot_active)
    private val drawableActiveRing = ContextCompat.getDrawable(context, R.drawable.ic_pattern_ring_active)
    private val drawableErrorDot = ContextCompat.getDrawable(context, R.drawable.ic_pattern_dot_error)
    private val drawableErrorRing = ContextCompat.getDrawable(context, R.drawable.ic_pattern_ring_error)
    // Màu cho đường line nối
    private val colorLineActive = ContextCompat.getColor(context, R.color.pattern_active)
    private val colorLineError = ContextCompat.getColor(context, R.color.pattern_error)
    // Hàm tiện ích hỗ trợ vẽ Drawable với tâm (cx, cy) và bán kính radius
    private fun drawDrawable(canvas: Canvas, drawable: Drawable?, cx: Float, cy: Float, radius: Float) {
        drawable?.let {
            it.setBounds(
                (cx - radius).toInt(),
                (cy - radius).toInt(),
                (cx + radius).toInt(),
                (cy + radius).toInt()
            )
            it.draw(canvas)
        }
    }

    // Paint đối tượng vẽ
    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePath = Path()

    // Kích thước hình học
    private var cellSize = 0f
    private var gridOffsetX = 0f
    private var gridOffsetY = 0f
    private var dotRadius = 0f
    private var selectedDotRadius = 0f
    private var ringRadius = 0f
    private var hitRadius = 0f

    init {
        isClickable = true
    }

    fun setOnPatternListener(listener: OnPatternListener) {
        this.patternListener = listener
    }

    fun setPatternState(state: PatternViewState) {
        this.viewState = state
        invalidate()
    }

    fun setInputEnabled(enabled: Boolean) {
        this.isInputEnabled = enabled
    }

    fun clearPattern() {
        selectedNodes.clear()
        linePath.reset()
        isDrawing = false
        viewState = PatternViewState.NORMAL
        patternListener?.onPatternCleared()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height
        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width
        } else {
            min(width, height)
        }
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val availableWidth = (w - paddingLeft - paddingRight).toFloat()
        val availableHeight = (h - paddingTop - paddingBottom).toFloat()
        val minDim = min(availableWidth, availableHeight)

        cellSize = minDim / 3f
        gridOffsetX = paddingLeft + (availableWidth - minDim) / 2f
        gridOffsetY = paddingTop + (availableHeight - minDim) / 2f

        dotRadius = cellSize * 0.08f
        selectedDotRadius = cellSize * 0.10f
        ringRadius = cellSize * 0.24f
        hitRadius = cellSize * 0.35f
        paintLine.strokeWidth = cellSize * 0.06f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInputEnabled) return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Ngăn ViewPager2 hoặc ViewGroup cha cướp touch event khi bắt đầu thao tác
                parent?.requestDisallowInterceptTouchEvent(true)

                // Nếu đang ở trạng thái lỗi từ lần trước mà chưa clear, chạm mới sẽ xóa hình cũ
                if (viewState == PatternViewState.ERROR) {
                    clearPattern()
                }
                val hitIndex = findHitNode(x, y)
                if (hitIndex != -1) {
                    isDrawing = true
                    selectedNodes.add(hitIndex)
                    currentTouchX = x
                    currentTouchY = y
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    patternListener?.onPatternStarted()
                    patternListener?.onPatternProgress(selectedNodes.toList())
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Duy trì việc chặn ViewGroup cha can thiệp trong suốt quá trình kéo vẽ
                parent?.requestDisallowInterceptTouchEvent(true)
                currentTouchX = x
                currentTouchY = y

                val hitIndex = findHitNode(x, y)
                // Nếu chưa bắt đầu vẽ nhưng ngón tay lướt trúng điểm đầu tiên
                if (!isDrawing) {
                    if (hitIndex != -1) {
                        isDrawing = true
                        selectedNodes.add(hitIndex)
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        patternListener?.onPatternStarted()
                        patternListener?.onPatternProgress(selectedNodes.toList())
                        invalidate()
                    }
                    return true
                }

                // Đang vẽ và lướt qua các điểm tiếp theo
                if (hitIndex != -1 && !selectedNodes.contains(hitIndex)) {
                    val lastIndex = selectedNodes.last()
                    val intermediate = getIntermediateNode(lastIndex, hitIndex)
                    if (intermediate != null && !selectedNodes.contains(intermediate)) {
                        selectedNodes.add(intermediate)
                    }

                    selectedNodes.add(hitIndex)
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    patternListener?.onPatternProgress(selectedNodes.toList())
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Trả lại quyền touch cho ViewGroup cha
                parent?.requestDisallowInterceptTouchEvent(false)
                // Khi người dùng nhấc tay ra: tính là 1 lần vẽ hoàn thành
                if (isDrawing || selectedNodes.isNotEmpty()) {
                    isDrawing = false
                    val patternResult = selectedNodes.toList()
                    if (patternResult.isNotEmpty()) {
                        patternListener?.onPatternCompleted(patternResult)
                    }
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // Trả lại quyền touch cho ViewGroup cha
                parent?.requestDisallowInterceptTouchEvent(false)
                // Khi cử chỉ bị ngắt: chỉ hủy và reset hình vẽ, không gửi kết quả hoàn thành
                isDrawing = false
                clearPattern()
                return true
            }
        }
        return super.onTouchEvent(event)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val isError = viewState == PatternViewState.ERROR
        val lineCurrentColor = if (isError) colorLineError else colorLineActive

        // 1. Vẽ các đường nối giữa các điểm đã chọn
        if (selectedNodes.isNotEmpty()) {
            paintLine.color = lineCurrentColor
            linePath.reset()
            val firstCenter = getNodeCenter(selectedNodes[0])
            linePath.moveTo(firstCenter.first, firstCenter.second)

            for (i in 1 until selectedNodes.size) {
                val center = getNodeCenter(selectedNodes[i])
                linePath.lineTo(center.first, center.second)
            }

            // Vẽ tiếp từ điểm cuối tới vị trí ngón tay nếu đang drag
            if (isDrawing) {
                linePath.lineTo(currentTouchX, currentTouchY)
            }
            canvas.drawPath(linePath, paintLine)
        }

        // 2. Vẽ 9 node trực tiếp bằng Drawable
        val ringDrawable = if (isError) drawableErrorRing else drawableActiveRing
        val dotSelectedDrawable = if (isError) drawableErrorDot else drawableActiveDot

        for (i in 0 until 9) {
            val center = getNodeCenter(i)
            val cx = center.first
            val cy = center.second
            val isSelected = selectedNodes.contains(i)

            if (isSelected) {
                // Vẽ vòng ngoài
                drawDrawable(canvas, ringDrawable, cx, cy, ringRadius)
                // Vẽ chấm tâm nổi bật
                drawDrawable(canvas, dotSelectedDrawable, cx, cy, selectedDotRadius)
            } else {
                // Vẽ chấm nhàn rỗi chưa chọn
                drawDrawable(canvas, drawableNormalDot, cx, cy, dotRadius)
            }
        }
    }

    // Lấy toạ độ tâm (cx, cy) của node thứ index
    private fun getNodeCenter(index: Int): Pair<Float, Float> {
        val row = index / 3
        val col = index % 3
        val cx = gridOffsetX + col * cellSize + cellSize / 2f
        val cy = gridOffsetY + row * cellSize + cellSize / 2f
        return Pair(cx, cy)
    }

    // Tìm node gần vị trí chạm ngón tay
    private fun findHitNode(touchX: Float, touchY: Float): Int {
        for (i in 0 until 9) {
            val center = getNodeCenter(i)
            val dx = touchX - center.first
            val dy = touchY - center.second
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (distance <= hitRadius) {
                return i
            }
        }
        return -1
    }

    // Tính toán node trung gian giữa 2 node nếu thẳng hàng
    private fun getIntermediateNode(from: Int, to: Int): Int? {
        val fromRow = from / 3
        val fromCol = from % 3
        val toRow = to / 3
        val toCol = to % 3
        val diffRow = kotlin.math.abs(toRow - fromRow)
        val diffCol = kotlin.math.abs(toCol - fromCol)

        if ((diffRow == 2 || diffRow == 0) && (diffCol == 2 || diffCol == 0)) {
            val midRow = (fromRow + toRow) / 2
            val midCol = (fromCol + toCol) / 2
            val midIndex = midRow * 3 + midCol
            if (midIndex != from && midIndex != to) {
                return midIndex
            }
        }
        return null
    }
}
