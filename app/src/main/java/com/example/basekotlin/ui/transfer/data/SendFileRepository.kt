package com.example.basekotlin.ui.transfer.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.format.Formatter
import com.example.basekotlin.ui.transfer.model.BaseFileItem
import com.example.basekotlin.ui.transfer.model.FileCategory
import com.example.basekotlin.ui.transfer.model.TransferableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SendFileRepository(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)



    // 1. Quét Video (Lấy BUCKET_DISPLAY_NAME làm folder)
    fun queryVideos(): List<TransferableItem> {
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, "${MediaStore.Video.Media.SIZE} > 0", null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val size = c.getLong(sizeCol)
                val dateMod = c.getLong(dateCol) * 1000L
                val name = c.getString(nameCol) ?: "Video"
                val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Others" else "Others"

                list.add(BaseFileItem(
                    id = uri.toString(),
                    displayName = name,
                    subInfo = "${Formatter.formatShortFileSize(context, size)} - ${dateFormat.format(Date(dateMod))}",
                    sizeBytes = size,
                    dateModifiedMillis = dateMod,
                    mimeType = c.getString(mimeCol) ?: "video/*",
                    uri = uri,
                    category = FileCategory.VIDEO,
                    thumbnailUri = uri,
                    groupKey = folder
                ))
            }
        }
        return list
    }

    // 2. Quét Photo
    fun queryPhotos(): List<TransferableItem> {
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, "${MediaStore.Images.Media.SIZE} > 0", null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val size = c.getLong(sizeCol)
                val dateMod = c.getLong(dateCol) * 1000L
                val name = c.getString(nameCol) ?: "Photo"
                val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Others" else "Others"

                list.add(BaseFileItem(
                    id = uri.toString(),
                    displayName = name,
                    subInfo = "${Formatter.formatShortFileSize(context, size)} - ${dateFormat.format(Date(dateMod))}",
                    sizeBytes = size,
                    dateModifiedMillis = dateMod,
                    mimeType = c.getString(mimeCol) ?: "image/*",
                    uri = uri,
                    category = FileCategory.PHOTO,
                    thumbnailUri = uri,
                    groupKey = folder
                ))
            }
        }
        return list
    }

    // 3. Quét Contacts (Gom nhóm theo chữ cái A..Z)
    fun queryContacts(): List<TransferableItem> {
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoCol = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (c.moveToNext()) {
                val id = c.getString(idCol)
                val name = c.getString(nameCol) ?: "Unknown"
                val phone = c.getString(numCol) ?: ""
                val photo = c.getString(photoCol)?.let { Uri.parse(it) }
                val firstChar = name.firstOrNull()?.uppercaseChar()?.toString()?.takeIf { it in "A".."Z" } ?: "#"

                list.add(BaseFileItem(
                    id = "contact_$id",
                    displayName = name,
                    subInfo = phone,
                    sizeBytes = 0L,
                    dateModifiedMillis = 0L,
                    mimeType = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLong()),
                    category = FileCategory.CONTACTS,
                    thumbnailUri = photo,
                    fallbackLetter = firstChar,
                    groupKey = firstChar
                ))
            }
        }
        return list
    }

    // 4. Quét Files Tài Liệu (PDF, EXCEL, PPT, TXT, DOC, WPS, ZIP)
    fun queryDocuments(): List<TransferableItem> {
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.doc' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.xls' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.ppt' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.wps' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.zip'"

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, null,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (c.moveToNext()) {
                val path = c.getString(dataCol) ?: continue
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                val size = c.getLong(sizeCol)
                val dateMod = c.getLong(dateCol) * 1000L
                val name = c.getString(nameCol) ?: File(path).name
                val ext = File(path).extension.uppercase()
                val groupType = when (ext) {
                    "PDF" -> "PDF"
                    "XLS", "XLSX", "CSV" -> "EXCEL"
                    "PPT", "PPTX" -> "PPT"
                    "DOC", "DOCX" -> "DOC"
                    "TXT" -> "TXT"
                    "WPS" -> "WPS"
                    "ZIP", "RAR", "7Z" -> "ZIP"
                    else -> "OTHERS"
                }

                list.add(BaseFileItem(
                    id = uri.toString(),
                    displayName = name,
                    subInfo = "${Formatter.formatShortFileSize(context, size)} - ${dateFormat.format(Date(dateMod))}",
                    sizeBytes = size,
                    dateModifiedMillis = dateMod,
                    mimeType = "application/*",
                    uri = uri,
                    category = FileCategory.FILES,
                    thumbnailUri = null,
                    groupKey = groupType
                ))
            }
        }
        return list
    }

    // 5. Quét Music (Gom nhóm theo chữ cái bài hát A..Z)
    fun queryMusic(): List<TransferableItem> {
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM_ID
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.SIZE} > 0", null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val fileNameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val title = c.getString(titleCol) ?: "Music"
                val fileName = c.getString(fileNameCol) ?: "music.mp3"
                val size = c.getLong(sizeCol)
                val dateMod = c.getLong(dateCol) * 1000L
                val firstChar = title.firstOrNull()?.uppercaseChar()?.toString()?.takeIf { it in "A".."Z" } ?: "#"

                list.add(BaseFileItem(
                    id = uri.toString(),
                    displayName = fileName,
                    subInfo = "${Formatter.formatShortFileSize(context, size)} - ${dateFormat.format(Date(dateMod))}",
                    sizeBytes = size,
                    dateModifiedMillis = dateMod,
                    mimeType = "audio/*",
                    uri = uri,
                    category = FileCategory.MUSIC,
                    thumbnailUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.getLong(albumIdCol)),
                    fallbackLetter = firstChar,
                    groupKey = firstChar
                ))
            }
        }
        return list
    }

    // 6. Quét Apps đã cài
    fun queryInstalledApps(): List<TransferableItem> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                val file = File(appInfo.sourceDir)
                if (!file.exists()) return@mapNotNull null
                val uri = Uri.fromFile(file)
                val appLabel = pm.getApplicationLabel(appInfo).toString()

                BaseFileItem(
                    id = appInfo.packageName,
                    displayName = "${appLabel}.apk",
                    subInfo = Formatter.formatShortFileSize(context, file.length()),
                    sizeBytes = file.length(),
                    dateModifiedMillis = file.lastModified(),
                    mimeType = "application/vnd.android.package-archive",
                    uri = uri,
                    category = FileCategory.APPS,
                    groupKey = "Installed"
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    // 7. Quét File APK chưa cài
    fun queryNotInstalledApks(): List<TransferableItem> {
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(0).map { it.packageName }.toSet()
        val list = mutableListOf<TransferableItem>()
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE)
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk' AND ${MediaStore.Files.FileColumns.SIZE} > 0"

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, null, null
        )?.use { c ->
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (c.moveToNext()) {
                val apkPath = c.getString(dataCol) ?: continue
                val file = File(apkPath)
                if (!file.exists()) continue
                val pkgInfo = pm.getPackageArchiveInfo(apkPath, 0) ?: continue
                val pkgName = pkgInfo.packageName ?: continue
                if (!installedPackages.contains(pkgName)) {
                    val appName = pkgInfo.applicationInfo?.let {
                        it.sourceDir = apkPath
                        it.publicSourceDir = apkPath
                        pm.getApplicationLabel(it).toString()
                    } ?: file.nameWithoutExtension

                    list.add(BaseFileItem(
                        id = apkPath,
                        displayName = "${appName}.apk",
                        subInfo = Formatter.formatShortFileSize(context, c.getLong(sizeCol)),
                        sizeBytes = c.getLong(sizeCol),
                        dateModifiedMillis = file.lastModified(),
                        mimeType = "application/vnd.android.package-archive",
                        uri = Uri.fromFile(file),
                        category = FileCategory.APPS,
                        groupKey = "NotInstalled"
                    ))
                }
            }
        }
        return list.sortedBy { it.displayName.lowercase() }
    }

    // 8. Quét Lịch Sử Gửi / Nhận (Recent)
    fun queryRecentSent(): List<TransferableItem> {
        // Tương tự query từ database room/lịch sử chuyển file của app
        return emptyList()
    }

    fun queryRecentReceived(): List<TransferableItem> {
        return emptyList()
    }
}
