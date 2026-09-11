package com.example.basekotlin.ui.cleanfile.messenger

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.data.local.cleanfile.MessengerMediaScanner
import com.example.basekotlin.databinding.ActivityMessengerCleanerBinding
import com.example.basekotlin.ui.cleanfile.CleanFileUtils
import com.example.basekotlin.ui.cleanfile.messenger.adapter.CleanerCategoryAdapter
import com.example.basekotlin.ui.cleanfile.messenger.detail.CleanerDetailActivity
import kotlinx.coroutines.launch

class MessengerCleanerActivity : BaseActivity<ActivityMessengerCleanerBinding>(ActivityMessengerCleanerBinding::inflate) {

    companion object {
        const val EXTRA_MESSENGER_TYPE = "extra_messenger_type"
    }

    private val viewModel: MessengerCleanerViewModel by viewModels()
    private val adapter by lazy { CleanerCategoryAdapter() }
    private var messengerType = MessengerMediaScanner.MESSENGER_TELEGRAM

    override fun getData() {
        messengerType = intent.getStringExtra(EXTRA_MESSENGER_TYPE) ?: MessengerMediaScanner.MESSENGER_TELEGRAM
    }

    override fun initView() {
        val appName = if (messengerType == MessengerMediaScanner.MESSENGER_TELEGRAM) "Telegram" else "WhatsApp"
        binding.tvTitle.text = if (messengerType == MessengerMediaScanner.MESSENGER_TELEGRAM) {
            getString(R.string.telegram_cleaner)
        } else {
            getString(R.string.whatsapp_cleaner)
        }
        binding.tvHeaderDesc.text = "Free up $appName storage space by clearing cached and sent media files"

        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        adapter.onCategoryClick = { category ->
            val bundle = Bundle().apply {
                putString(CleanerDetailActivity.EXTRA_MESSENGER_TYPE, messengerType)
                putString(CleanerDetailActivity.EXTRA_CATEGORY_ID, category.id)
                putString(CleanerDetailActivity.EXTRA_CATEGORY_NAME, category.name)
            }
            startNextActivity(CleanerDetailActivity::class.java, bundle)
        }

        adapter.onCleanJunkClick = {
            viewModel.cleanJunk { success ->
                val msg = if (success) getString(R.string.junk_clean_success) else getString(R.string.junk_clean_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun bindView() {
        binding.btnBack.tap {
            finishThisActivity()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { cats ->
                    adapter.addListData(cats.toMutableList())
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalSizeBytes.collect { size ->
                    binding.tvTotalSize.text = CleanFileUtils.formatFileSize(size)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    if (loading) binding.progressBar.visible() else binding.progressBar.gone()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCategories(messengerType)
    }
}
