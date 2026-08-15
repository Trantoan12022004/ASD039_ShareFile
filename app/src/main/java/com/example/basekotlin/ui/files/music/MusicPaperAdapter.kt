package com.example.basekotlin.ui.files.music

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.files.music.fragment.AllFragment
import com.example.basekotlin.ui.files.music.fragment.FavoriteFragment
import com.example.basekotlin.ui.files.music.fragment.FoldersFragment
import com.example.basekotlin.ui.files.music.fragment.PlaylistFragment
import com.example.basekotlin.ui.files.music.fragment.RecentlyPlayedFragment

class MusicPagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 6
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return AllFragment()
        } else if (position == 1) {
            return AllFragment()
        } else if (position == 2) {
            return FoldersFragment()
        } else if (position == 3) {
            return FavoriteFragment()
        } else if (position == 4) {
            return RecentlyPlayedFragment()
        } else {
            return PlaylistFragment()
        }
    }
}