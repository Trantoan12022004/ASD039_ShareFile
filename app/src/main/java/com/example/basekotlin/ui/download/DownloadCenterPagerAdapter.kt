package com.example.basekotlin.ui.download

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.download.fragment.DownloadCenterFragment
import com.example.basekotlin.ui.download.safebox.DownloadSafeBoxFragment

class DownloadCenterPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DownloadSafeBoxFragment.newInstance()
            else -> DownloadCenterFragment.newInstance(position)
        }
    }
}