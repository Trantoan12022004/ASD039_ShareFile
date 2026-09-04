package com.example.basekotlin.ui.files.zips

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.files.pdfconverter.fragment.ImgToPdfFragment
import com.example.basekotlin.ui.files.zips.fragment.UnzippedFragment
import com.example.basekotlin.ui.files.zips.fragment.ZippedFragment

class ZipsPagerAdapter (activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return ZippedFragment()
        } else {
            return UnzippedFragment()
        }
    }
}