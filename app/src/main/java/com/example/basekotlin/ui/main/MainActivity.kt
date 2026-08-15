package com.example.basekotlin.ui.main
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.tap
import com.example.basekotlin.databinding.ActivityMainBinding
import com.example.basekotlin.dialog.exit.ExitAppDialog
import com.example.basekotlin.ui.files.FilesActivity
import com.example.basekotlin.ui.language.LanguageActivity
import com.example.basekotlin.ui.setting.SettingActivity

@UnstableApi
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    override fun initView() {
    }
    override fun bindView() {
        binding.viewTop.btnFiles.tap {
            startNextActivity(FilesActivity::class.java, null)
        }

    }
    private fun showDialogQuit() {
        val dialogQuit = ExitAppDialog(this, false, onClick = { finishAffinity() })
        dialogQuit.show()
    }
    override fun onBack() {
    }
}
