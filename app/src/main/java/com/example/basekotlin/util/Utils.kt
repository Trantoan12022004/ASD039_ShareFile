package com.example.basekotlin.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import java.io.IOException

object Utils {
    const val STORAGE = "STORAGE"

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getListPathFromAssets(context: Context, dirFrom: String): ArrayList<String> {
        val listBackground: ArrayList<String> = ArrayList<String>()
        val res: Resources = context.resources
        val am = res.assets
        var fileList: Array<String?>? = arrayOfNulls(0)
        try {
            fileList = am.list(dirFrom)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (fileList != null) {
            for (s in fileList) {
                listBackground.add("file:///android_asset/$dirFrom/$s")
            }
        }
        return listBackground
    }
}