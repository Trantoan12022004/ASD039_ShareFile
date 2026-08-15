package com.example.basekotlin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.basekotlin.ui.splash.SplashActivity
import com.example.basekotlin.ui.welcome.WelcomeActivity
import com.example.basekotlin.util.NotificationHelper
import com.example.basekotlin.util.SharedPreUtils
import com.facebook.FacebookSdk


class MyApplication : Application(),
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    private var isStartApp = true

    companion object {
        private lateinit var appContext: Context
        private const val TAG = "MyApplication"

    }

    private var currentActivity: Activity? = null


    override fun onCreate() {
        super<Application>.onCreate()

        SharedPreUtils.init(this)

        val builder = StrictMode.VmPolicy.Builder()
        builder.detectFileUriExposure()
        StrictMode.setVmPolicy(builder.build())

        // --- Lifecycle listeners ---
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    }

    // Khi app quay lại foreground (VD: user thoát app -> vào lại)
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App returned to foreground: $currentActivity")

        if (currentActivity != null &&
            currentActivity !is SplashActivity &&
            currentActivity !is WelcomeActivity
        ) {
            NotificationHelper.showRandomNotification(this)
            showWelcomeBackScreen(currentActivity!!)
        }
    }

    private fun showWelcomeBackScreen(fromActivity: Activity, isFromRequestPer: Boolean = false) {
        try {
            val intent = Intent(fromActivity, WelcomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isFromRequestPer) intent.putExtra("from_request_per", true)
            fromActivity.startActivity(intent)

            Log.d(TAG, "WelcomeBackActivity started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting WelcomeBackActivity", e)
        }
    }

    // === Activity Lifecycle ===
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isStartApp) {
            if (activity::class.java.name != SplashActivity::class.java.name) {
                activity.startActivity(Intent(activity, SplashActivity::class.java))
                activity.finishAffinity()
                return
            }
        }
        isStartApp = false
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}