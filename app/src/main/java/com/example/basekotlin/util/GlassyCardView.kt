package com.example.basekotlin.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.example.basekotlin.R
import kotlin.apply

// GlassyCardView.kt
class GlassyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cornerRadius = 16f.dp(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var glassBackground = "#2914002D".toColorInt() // default

    init {
        // Đọc các attr từ XML
        context.obtainStyledAttributes(attrs, R.styleable.GlassyCardView).apply {
            cornerRadius = getDimension(
                R.styleable.GlassyCardView_cornerRadius, 16f.dp(context)
            )
            glassBackground = getColor(
                R.styleable.GlassyCardView_glassBackground,
                "#2914002D".toColorInt()  // fallback nếu không set trong XML
            )
            recycle() // ← bắt buộc
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val rect = RectF(0f, 0f, w, h)
        val path = Path().apply { addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW) }

        // 1. Nền glass
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = glassBackground
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // 2. Gradient fill (ánh sáng từ trên)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.5f,
            intArrayOf(Color.argb(35, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.shader = null

        // 3. Border với LinearGradient theo chiều ngang
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f.dp(context)

        // Cạnh trên: sáng → giữ sáng lâu → mờ dần ở 70% cuối
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                Color.argb(180, 255, 255, 255),  // trái: sáng
                Color.argb(180, 255, 255, 255),  // 70%: vẫn còn sáng
                Color.argb(0, 255, 255, 255)     // phải: mờ hẳn
            ),
            floatArrayOf(0f, 0.9f, 1f),          // ← bắt đầu mờ từ 70%
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(cornerRadius, 0f, w - cornerRadius, 0f, paint)

        // Cạnh dưới: đảo lại → mờ ở 30% đầu, sáng từ 30% → phải
        paint.shader = LinearGradient(
            0f, h, w, h,
            intArrayOf(
                Color.argb(0, 255, 255, 255),    // trái: mờ hẳn
                Color.argb(180, 255, 255, 255),  // 30%: bắt đầu sáng
                Color.argb(180, 255, 255, 255)   // phải: sáng
            ),
            floatArrayOf(0f, 0.1f, 1f),          // ← sáng từ 30%
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(cornerRadius, h, w - cornerRadius, h, paint)

        // Cạnh trái: sáng trên → mờ dưới
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(180, 255, 255, 255),  // trên: sáng
                Color.argb(0, 255, 255, 255)     // dưới: mờ hẳn
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(0f, cornerRadius, 0f, h - cornerRadius, paint)

        // Cạnh phải: mờ trên → sáng dưới (đảo lại)
        paint.shader = LinearGradient(
            w, 0f, w, h,
            intArrayOf(
                Color.argb(0, 255, 255, 255),    // trên: mờ hẳn
                Color.argb(180, 255, 255, 255)   // dưới: sáng
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(w, cornerRadius, w, h - cornerRadius, paint)
        paint.shader = null

        // 4. Bo góc — alpha khớp với 2 cạnh nối
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f.dp(context)

        // Góc trên trái: left sáng (180) → top sáng (180) → cùng alpha, solid luôn
        paint.color = Color.argb(60, 255, 255, 255)  // 180 → 80
        canvas.drawArc(RectF(0f, 0f, cornerRadius * 2, cornerRadius * 2), 180f, 90f, false, paint)

        // Góc trên phải: top mờ (0) → right mờ (0) → cùng alpha, solid luôn
        paint.color = Color.argb(20, 255, 255, 255)
        canvas.drawArc(RectF(w - cornerRadius * 2, 0f, w, cornerRadius * 2), 270f, 90f, false, paint)

        // Góc dưới phải: right sáng (180) → bottom sáng (180) → cùng alpha, solid luôn
        paint.color = Color.argb(60, 255, 255, 255)  // 180 → 80
        canvas.drawArc(RectF(w - cornerRadius * 2, h - cornerRadius * 2, w, h), 0f, 90f, false, paint)

        // Góc dưới trái: bottom mờ (0) → left mờ (0) → cùng alpha, solid luôn
        paint.color = Color.argb(20, 255, 255, 255)
        canvas.drawArc(RectF(0f, h - cornerRadius * 2, cornerRadius * 2, h), 90f, 90f, false, paint)
    }

    // Blur thật bằng RenderEffect (API 31+)
    fun applyBlur(blurRadius: Float = 20f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadius, blurRadius,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }

    private fun Float.dp(ctx: Context) =
        this * ctx.resources.displayMetrics.density
}