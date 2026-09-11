package com.example.basekotlin.data.local.cleanfile

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import com.example.basekotlin.R
import com.example.basekotlin.model.CleanFileItem
import com.example.basekotlin.model.CleanFileType
import com.example.basekotlin.model.MessengerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MessengerMediaScanner {

    const val MESSENGER_TELEGRAM = "telegram"
    const val MESSENGER_WHATSAPP = "whatsapp"

    const val CAT_PHOTOS = "photos"
    const val CAT_VIDEOS = "videos"
    const val CAT_AUDIOS = "audios"
    const val CAT_FILES = "files"
    const val CAT_JUNK = "junk"

    fun isMessengerInstalled(context: Context, messenger: String): Boolean {
        val packages = if (messenger == MESSENGER_TELEGRAM) {
            listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")
        } else {
            listOf("com.whatsapp", "com.whatsapp.w4b")
        }
        val pm = context.packageManager
        for (pkg in packages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return false
    }

    suspend fun scanCategories(context: Context, messenger: String): List<MessengerCategory> {
        return withContext(Dispatchers.IO) {
            val categories = listOf(
                MessengerCategory(
                    id = CAT_PHOTOS,
                    name = "Photos",
                    description = "Clean up photos in chats",
                    iconRes = R.drawable.ic_cat_photos
                ),
                MessengerCategory(
                    id = CAT_VIDEOS,
                    name = "Videos",
                    description = "Clean up videos in chats",
                    iconRes = R.drawable.ic_cat_videos
                ),
                MessengerCategory(
                    id = CAT_AUDIOS,
                    name = "Audios",
                    description = "Clean up voice and audios",
                    iconRes = R.drawable.ic_cat_music
                ),
                MessengerCategory(
                    id = CAT_FILES,
                    name = "Files",
                    description = "Clean up received documents",
                    iconRes = R.drawable.ic_cat_document
                ),
                MessengerCategory(
                    id = CAT_JUNK,
                    name = "Junk Files",
                    description = "Temporary cache files",
                    iconRes = R.drawable.junk,
                    isJunk = true
                )
            )

            for (cat in categories) {
                val files = scanCategoryFiles(context, messenger, cat.id)
                cat.fileCount = files.size
                cat.sizeBytes = files.sumOf { it.sizeBytes }
                cat.hasJunk = cat.sizeBytes > 0
            }

            categories
        }
    }

    suspend fun scanCategoryFiles(
        context: Context,
        messenger: String,
        categoryId: String
    ): List<CleanFileItem> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<CleanFileItem>()
            val targetDirs = getTargetDirectories(messenger, categoryId)

            for (dir in targetDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        if (file.isFile && file.length() > 0) {
                            val type = resolveCategoryType(categoryId, file.name)
                            result.add(
                                CleanFileItem(
                                    name = file.name,
                                    path = file.absolutePath,
                                    sizeBytes = file.length(),
                                    dateModifiedMillis = file.lastModified(),
                                    type = type,
                                    thumbnailUri = "file://${file.absolutePath}",
                                    folderName = dir.name,
                                    mimeType = null
                                )
                            )
                        }
                    }
                }
            }

            result.sortedByDescending { it.sizeBytes }
        }
    }

    private fun resolveCategoryType(categoryId: String, fileName: String): CleanFileType {
        return when (categoryId) {
            CAT_PHOTOS -> CleanFileType.PHOTO
            CAT_VIDEOS -> CleanFileType.VIDEO
            CAT_AUDIOS -> CleanFileType.AUDIO
            CAT_FILES -> CleanFileType.DOCUMENT
            else -> BigFileScanner.resolveFileType(fileName, null)
        }
    }

    private fun getTargetDirectories(messenger: String, categoryId: String): List<File> {
        val root = Environment.getExternalStorageDirectory()
        val dirs = mutableListOf<File>()

        if (messenger == MESSENGER_TELEGRAM) {
            val baseTelegram = File(root, "Telegram")
            val mediaTelegram = File(root, "Android/media/org.telegram.messenger/Telegram")
            val picturesTelegram = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Telegram")
            val moviesTelegram = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Telegram")

            when (categoryId) {
                CAT_PHOTOS -> {
                    dirs.add(File(baseTelegram, "Telegram Images"))
                    dirs.add(File(mediaTelegram, "Telegram Images"))
                    dirs.add(picturesTelegram)
                }
                CAT_VIDEOS -> {
                    dirs.add(File(baseTelegram, "Telegram Video"))
                    dirs.add(File(mediaTelegram, "Telegram Video"))
                    dirs.add(moviesTelegram)
                }
                CAT_AUDIOS -> {
                    dirs.add(File(baseTelegram, "Telegram Audio"))
                    dirs.add(File(mediaTelegram, "Telegram Audio"))
                }
                CAT_FILES -> {
                    dirs.add(File(baseTelegram, "Telegram Documents"))
                    dirs.add(File(mediaTelegram, "Telegram Documents"))
                }
                CAT_JUNK -> {
                    dirs.add(File(root, "Android/data/org.telegram.messenger/cache"))
                }
            }
        } else {
            // WhatsApp
            val legacyMedia = File(root, "WhatsApp/Media")
            val scopedMedia = File(root, "Android/media/com.whatsapp/WhatsApp/Media")

            when (categoryId) {
                CAT_PHOTOS -> {
                    dirs.add(File(legacyMedia, "WhatsApp Images"))
                    dirs.add(File(scopedMedia, "WhatsApp Images"))
                }
                CAT_VIDEOS -> {
                    dirs.add(File(legacyMedia, "WhatsApp Video"))
                    dirs.add(File(scopedMedia, "WhatsApp Video"))
                    dirs.add(File(legacyMedia, "WhatsApp Animated Gifs"))
                    dirs.add(File(scopedMedia, "WhatsApp Animated Gifs"))
                }
                CAT_AUDIOS -> {
                    dirs.add(File(legacyMedia, "WhatsApp Audio"))
                    dirs.add(File(scopedMedia, "WhatsApp Audio"))
                    dirs.add(File(legacyMedia, "WhatsApp Voice Notes"))
                    dirs.add(File(scopedMedia, "WhatsApp Voice Notes"))
                }
                CAT_FILES -> {
                    dirs.add(File(legacyMedia, "WhatsApp Documents"))
                    dirs.add(File(scopedMedia, "WhatsApp Documents"))
                }
                CAT_JUNK -> {
                    dirs.add(File(root, "Android/data/com.whatsapp/cache"))
                }
            }
        }

        return dirs
    }
}
