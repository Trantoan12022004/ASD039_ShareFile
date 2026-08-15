package com.example.basekotlin.ui.intro

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tapNoHandle
import com.example.basekotlin.model.IntroModel
import com.example.basekotlin.databinding.ActivityIntroBinding
import com.example.basekotlin.ui.intro.adapter.IntroAdapter
import com.example.basekotlin.ui.main.MainActivity
import com.example.basekotlin.ui.permission.PermissionActivity
import com.example.basekotlin.util.SharedPreUtils
import kotlin.math.abs

class IntroActivity : BaseActivity<ActivityIntroBinding>(ActivityIntroBinding::inflate) {

    private var dots: Array<ImageView>? = null
    private var listIntro: ArrayList<IntroModel> = ArrayList()

    override fun initView() {
        dots = arrayOf(binding.ivCircle01, binding.ivCircle02, binding.ivCircle03)

        listIntro.add(
            IntroModel(
                R.string.title_intro_01, R.string.content_intro_01, R.drawable.intro_1
            )
        )
        listIntro.add(
            IntroModel(
                R.string.title_intro_02, R.string.content_intro_02, R.drawable.intro_2
            )
        )
        listIntro.add(
            IntroModel(
                R.string.title_intro_03, R.string.content_intro_03, R.drawable.intro_3
            )
        )

        val introAdapter = IntroAdapter()
        introAdapter.addListData(listIntro)
        binding.viewPager2.adapter = introAdapter

    }

    override fun bindView() {
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(100))
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.8f + r * 0.2f
            val absPosition = abs(position)
            page.alpha = 1.0f - (1.0f - 0.3f) * absPosition
        }
        binding.viewPager2.setPageTransformer(compositePageTransformer)
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                changeContentInit(position)

                when (position) {
                    0, 1 -> {
                        binding.btnNext.tapNoHandle {
                            binding.viewPager2.currentItem = binding.viewPager2.currentItem + 1
                        }
                    }

                    2 -> {
                        binding.btnNext.tapNoHandle {
                            startNextActivity()
                        }
                    }
                }
            }
        })
    }

    private fun startNextActivity() {
//        if (SharedPreUtils.getInstance().getCountOpenApp(this) > 1) {
//            startNextActivity(MainActivity::class.java, null)
//        } else {
//            startNextActivity(PermissionActivity::class.java, null)
//        }
        startNextActivity(PermissionActivity::class.java, null)
        finish()
    }

    private fun changeContentInit(position: Int) {
        for (i in 0..2) {
            if (i == position) dots!![i].setImageResource(R.drawable.ic_intro_s) else dots!![i].setImageResource(
                R.drawable.ic_intro_sn
            )
        }
    }

    override fun onStart() {
        super.onStart()
        changeContentInit(binding.viewPager2.currentItem)
    }

    override fun onBack() {
        finishAffinity()
    }
}