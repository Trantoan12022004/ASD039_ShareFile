package com.example.basekotlin.util

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

// Tăng ngưỡng nhận diện cử chỉ vuốt của ViewPager2 để tránh nhảy tab khi cuộn chéo
fun ViewPager2.reduceDragSensitivity(multiplier: Int = 3) {
    try {
        // Lấy RecyclerView bên trong ViewPager2
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val internalRecyclerView = recyclerViewField.get(this) as? RecyclerView

        if (internalRecyclerView != null) {
            // Lấy thuộc tính mTouchSlop của RecyclerView
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val currentTouchSlop = touchSlopField.get(internalRecyclerView) as? Int

            if (currentTouchSlop != null) {
                // Nhân ngưỡng touch slop lên để giảm độ nhạy vuốt ngang
                val newTouchSlop = currentTouchSlop * multiplier
                touchSlopField.set(internalRecyclerView, newTouchSlop)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
