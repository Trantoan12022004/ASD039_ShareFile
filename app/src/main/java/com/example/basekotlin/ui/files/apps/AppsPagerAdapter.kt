package com.example.basekotlin.ui.files.apps

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.files.apps.fragment.APKFragment
import com.example.basekotlin.ui.files.apps.fragment.InstalledFragment
import com.example.basekotlin.ui.files.apps.fragment.ReceivedFragment

class AppsPagerAdapter(activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 3
        }

        override fun createFragment(position: Int): Fragment {
            if (position == 0) {
                return ReceivedFragment()
            } else if (position == 1) {
                return APKFragment()
            } else {
                return InstalledFragment()
            }
        }
    }