package com.example.basekotlin.ui.files.video

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.files.photos.fragment.AllFolderPhotoFragment
import com.example.basekotlin.ui.files.photos.fragment.AllPhotosFragment
import com.example.basekotlin.ui.files.photos.fragment.PhotoReceiveFragment
import com.example.basekotlin.ui.files.video.fragment.AllFolderVideoFragment
import com.example.basekotlin.ui.files.video.fragment.AllVideoFragment
import com.example.basekotlin.ui.files.video.fragment.VideoReceiveFragment

class VideosPagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return AllVideoFragment()
        } else if (position == 1) {
            return AllFolderVideoFragment()
        } else {
            return VideoReceiveFragment()
        }
    }
}