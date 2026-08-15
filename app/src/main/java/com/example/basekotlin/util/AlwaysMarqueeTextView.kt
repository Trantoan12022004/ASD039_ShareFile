package com.example.basekotlin.util
import android.R
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView



class AlwaysMarqueeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    init {
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.MARQUEE
        marqueeRepeatLimit = -1
        isSelected = true
        isFocusable = true
        isFocusableInTouchMode = false
        setHorizontallyScrolling(true)

        attrs?.let { attributeSet ->
            val typedArray =
                context.obtainStyledAttributes(attributeSet, intArrayOf(R.attr.textAllCaps))
            val allCaps = typedArray.getBoolean(0, false)
            typedArray.recycle()

            if (allCaps) {
                text = text?.toString()?.uppercase()
            }
        }
    }
}