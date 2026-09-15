package com.example.basekotlin.ui.transfer.send

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.transfer.send.fragment.*

class SendTabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_COUNT = 7
        const val TAB_RECENT = 0
        const val TAB_CONTACTS = 1
        const val TAB_FILES = 2
        const val TAB_VIDEO = 3
        const val TAB_APPS = 4
        const val TAB_MUSIC = 5
        const val TAB_PHOTO = 6
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_RECENT -> VideosFragment() // Sẽ thay bằng RecentFragment
            TAB_CONTACTS -> ContactsFragment() // Sẽ thay bằng ContactsFragment
            TAB_FILES -> FilesFragment() // Sẽ thay bằng FilesFragment
            TAB_VIDEO -> VideosFragment() // Sẽ thay bằng VideoFragment
            TAB_APPS -> AppsFragment()
            TAB_MUSIC -> MusicsFragment() // Sẽ thay bằng MusicFragment
            TAB_PHOTO -> PicturesFragment() // Sẽ thay bằng PhotoFragment
            else -> AppsFragment()
        }
    }
}
