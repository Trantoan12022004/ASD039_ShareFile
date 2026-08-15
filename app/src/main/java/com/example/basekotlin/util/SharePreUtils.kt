package com.example.basekotlin.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.Gson

class SharedPreUtils {

    companion object {
        private const val PREF_PERMISSION_CLICK = "IS_COUNT_PERMISSION_CLICK"
        private const val KEY_NOTIFICATION_PERMISSION_CLICK = "NOTIFICATION_PERMISSION_CLICK"
        private const val KEY_MEMORY_PERMISSION_CLICK = "MEMORY_PERMISSION_CLICK"

        var mSharePref: SharedPreferences? = null
        fun init(context: Context) {
            if (mSharePref == null) {
                mSharePref = PreferenceManager.getDefaultSharedPreferences(context)
            }
        }

        @SuppressLint("StaticFieldLeak")
        private var instance: SharedPreUtils? = null
        fun getInstance(): SharedPreUtils {
            if (instance == null) {
                instance = SharedPreUtils()
            }
            return instance as SharedPreUtils
        }
    }

    fun clearAll() {
        mSharePref?.let {
            it.edit().clear().apply()
        }
    }

    fun removeValueWithKey(key: String) {
        mSharePref?.let {
            it.edit().remove(key).commit()
        }
    }

    fun setString(key: String, value: String) {
        mSharePref?.let {
            it.edit().putString(key, value).apply()
        }
    }

    fun getString(key: String, value: String): String? {
        mSharePref?.let {
            return it.getString(key, value)
        }
        return null
    }

    fun setInt(key: String, value: Int) {
        mSharePref?.let {
            it.edit().putInt(key, value).apply()
        }
    }

    fun getInt(key: String, value: Int): Int {
        mSharePref?.let {
            return it.getInt(key, value)
        }
        return -1
    }

    fun setLong(key: String, value: Long) {
        mSharePref?.let {
            it.edit().putLong(key, value).apply()
        }
    }

    fun getLong(key: String, value: Long): Long {
        mSharePref?.let {
            return it.getLong(key, value)
        }
        return -1
    }

    fun setBoolean(key: String, value: Boolean) {
        mSharePref?.let {
            it.edit().putBoolean(key, value).apply()
        }
    }

    fun getBoolean(key: String, value: Boolean): Boolean {
        return mSharePref?.getBoolean(key, value) ?: true
    }

    fun isRated(context: Context): Boolean {
        val pre = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        return pre.getBoolean("rated", false)
    }

    fun forceRated(context: Context) {
        val pre = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        val editor = pre.edit()
        editor.putBoolean("rated", true)
        editor.apply()
    }

    fun isFirstApp(context: Context): Boolean {
        val pre = context.getSharedPreferences("IS_FIRST_LANGUAGE", Context.MODE_PRIVATE)
        return pre.getBoolean("IS_FIRST_APP", false)
    }

    fun setFirstApp(context: Context) {
        val pre = context.getSharedPreferences("IS_FIRST_LANGUAGE", Context.MODE_PRIVATE)
        val editor = pre.edit()
        editor.putBoolean("IS_FIRST_APP", true)
        editor.apply()
    }

    fun getCountOpenApp(context: Context): Int {
        val pre = context.getSharedPreferences("IS_COUNT_OPEN_APP", Context.MODE_PRIVATE)
        return pre.getInt("IS_COUNT_OPEN_APP", 0)
    }

    fun setCountOpenApp(context: Context) {
        val pre = context.getSharedPreferences("IS_COUNT_OPEN_APP", Context.MODE_PRIVATE)
        val editor = pre.edit()
        editor.putInt("IS_COUNT_OPEN_APP", pre.getInt("IS_COUNT_OPEN_APP", 0) + 1)
        editor.apply()
    }

    fun setObject(context: Context, key: String?, value: Any?) {
        if (mSharePref == null) init(context)
        val gson = Gson()
        val json = gson.toJson(value)
        val editor = mSharePref!!.edit()
        editor.putString(key, json)
        editor.apply()
    }

    fun getObject(context: Context, key: String?, classObj: Class<*>?): Any {
        if (mSharePref == null) init(context)
        val gson = Gson()
        val json = mSharePref!!.getString(key, "")
        return gson.fromJson(json, classObj)
    }

    fun getCountPermissionClick(context: Context): Int {
        return getNotificationPermissionClickCount(context)
    }

    fun getNotificationPermissionClickCount(context: Context): Int {
        val pre = context.getSharedPreferences(PREF_PERMISSION_CLICK, Context.MODE_PRIVATE)
        val count = pre.getInt(KEY_NOTIFICATION_PERMISSION_CLICK, 0)
        return count
    }

    fun setNotificationPermissionClickCount(context: Context, count: Int) {
        val pre = context.getSharedPreferences(PREF_PERMISSION_CLICK, Context.MODE_PRIVATE)
        val editor = pre.edit()
        editor.putInt(KEY_NOTIFICATION_PERMISSION_CLICK, count)
        editor.apply()
    }

    fun getMemoryPermissionClickCount(context: Context): Int {
        val pre = context.getSharedPreferences(PREF_PERMISSION_CLICK, Context.MODE_PRIVATE)
        val count = pre.getInt(KEY_MEMORY_PERMISSION_CLICK, 0)
        return count
    }

    fun setMemoryPermissionClickCount(context: Context, count: Int) {
        val pre = context.getSharedPreferences(PREF_PERMISSION_CLICK, Context.MODE_PRIVATE)
        val editor = pre.edit()
        editor.putInt(KEY_MEMORY_PERMISSION_CLICK, count)
        editor.apply()
    }

}