package com.example.basekotlin.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.example.basekotlin.R
import com.example.basekotlin.base.gone
import com.example.basekotlin.dialog.rate.RatingDialog
import com.example.basekotlin.util.SettingManager.email
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory

object SettingManager {
    const val linkPolicy = ""
    const val email = "abcd@gmail.com"
}

fun Activity.shareApp() {
    val intentShare = Intent(Intent.ACTION_SEND)
    intentShare.type = "text/plain"
    intentShare.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.app_name))
    intentShare.putExtra(
        Intent.EXTRA_TEXT,
        "Download application :https://play.google.com/store/apps/details?id=${this.packageName}"
    )
    this.startActivity(Intent.createChooser(intentShare, "Share with"))
}

fun Activity.feedbackApp() {
    val uriText = """
                 mailto:$email?subject=Feedback for ${this.getString(R.string.app_name)}
                 &body=${this.getString(R.string.app_name)}
                 Feedback: 
                 """.trimIndent()
    val uri = Uri.parse(uriText)
    val sendIntent = Intent(Intent.ACTION_SENDTO)
    sendIntent.data = uri
    try {
        this.startActivity(
            Intent.createChooser(
                sendIntent, this.getString(R.string.Send_Email)
            )
        )
    } catch (_: Exception) {
        Toast.makeText(this, this.getString(R.string.feedback_failed), Toast.LENGTH_SHORT).show()
    }
}

fun Activity.rateApp(view: View?) {
    RatingDialog(this, onSend = { rating ->
        val uriText = """
                 mailto:${email}?subject=Review for ${this.getString(R.string.app_name)}&body=${
            this.getString(
                R.string.app_name
            )
        }
                 Rate : $rating
                 Content: 
                 """.trimIndent()
        val uri = Uri.parse(uriText)
        val sendIntent = Intent(Intent.ACTION_SENDTO)
        sendIntent.data = uri
        try {
            view!!.gone()
            this.startActivity(
                Intent.createChooser(
                    sendIntent, this.getString(R.string.Send_Email)
                )
            )
            SharedPreUtils.getInstance().forceRated(this)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this, this.getString(R.string.app_review_failed), Toast.LENGTH_SHORT
            ).show()
        }
    }, onRate = {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(
                    this, reviewInfo!!
                )
                flow.addOnSuccessListener {
                    SharedPreUtils.getInstance().forceRated(this)
                    view!!.gone()
                }
            }
        }
    }).show()
}