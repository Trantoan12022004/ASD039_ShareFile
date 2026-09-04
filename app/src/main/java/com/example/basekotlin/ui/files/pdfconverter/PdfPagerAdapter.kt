package com.example.basekotlin.ui.files.pdfconverter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.files.pdfconverter.fragment.ImgToPdfFragment
import com.example.basekotlin.ui.files.pdfconverter.fragment.PdfToImgFragment
import com.example.basekotlin.ui.files.photos.fragment.PhotoReceiveFragment

class PdfPagerAdapter (activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return PdfToImgFragment()
        } else {
            return ImgToPdfFragment()
        }
    }
}