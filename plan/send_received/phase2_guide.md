# PHASE 2: UI — Send Flow — Hướng Dẫn Code Chi Tiết

> **Mục tiêu**: Hoàn thiện giao diện chọn file (6 tabs) + QR Scanner + kết nối send flow.
>
> **Kết quả Phase 2**: User có thể chọn file từ 6 tabs → nhấn Send → scan QR → bắt đầu gửi file.

---

## Tổng Quan Files Cần Tạo Phase 2

| # | File | Loại | Chức năng |
|---|------|------|-----------|
| 1 | `activity_send_files.xml` | Layout | Layout chính cho SendFilesActivity |
| 2 | `fragment_send_recent.xml` | Layout | RecyclerView cho tab Recent |
| 3 | `fragment_send_photo.xml` | Layout | RecyclerView grid cho tab Photo |
| 4 | `item_send_file.xml` | Layout | Item file (Recent/File/Video/Music) |
| 5 | `item_send_photo.xml` | Layout | Item photo grid |
| 6 | `item_send_app.xml` | Layout | Item installed app |
| 7 | `activity_qr_scanner.xml` | Layout | Camera preview + scan overlay |
| 8 | `SendFilesActivity.kt` | Activity | Main activity chọn file |
| 9 | `SendFilesViewModel.kt` | ViewModel | Quản lý state chọn file |
| 10 | `RecentFragment.kt` | Fragment | Tab Recent |
| 11 | `InstalledAppsFragment.kt` | Fragment | Tab Installed |
| 12 | `FileFragment.kt` | Fragment | Tab File (Documents) |
| 13 | `PhotoFragment.kt` | Fragment | Tab Photo |
| 14 | `VideoFragment.kt` | Fragment | Tab Video |
| 15 | `MusicFragment.kt` | Fragment | Tab Music |
| 16 | `FileSelectionAdapter.kt` | Adapter | Adapter cho Recent/File/Video/Music |
| 17 | `PhotoSelectionAdapter.kt` | Adapter | Grid adapter cho Photo |
| 18 | `AppSelectionAdapter.kt` | Adapter | Adapter cho Installed Apps |
| 19 | `SendTabPagerAdapter.kt` | Adapter | ViewPager2 adapter |
| 20 | `QrScannerActivity.kt` | Activity | Camera QR scanner |

---

## Bước 2.1: Drawable Resources Cần Tạo

### File: `res/drawable/bg_send_button.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/primary_35" />
    <corners android:radius="24dp" />
</shape>
```

### File: `res/drawable/bg_send_button_disabled.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#99A1B0" />
    <corners android:radius="24dp" />
</shape>
```

### File: `res/drawable/bg_bottom_send_bar.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/white" />
    <corners
        android:topLeftRadius="16dp"
        android:topRightRadius="16dp" />
    <stroke android:width="0.5dp" android:color="#1A000000" />
</shape>
```

### File: `res/drawable/bg_photo_checkbox.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true">
        <shape android:shape="oval">
            <solid android:color="@color/primary_35" />
            <size android:width="24dp" android:height="24dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <stroke android:width="2dp" android:color="@color/white" />
            <solid android:color="#33000000" />
            <size android:width="24dp" android:height="24dp" />
        </shape>
    </item>
</selector>
```

### File: `res/drawable/bg_qr_scan_overlay.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <stroke android:width="3dp" android:color="@color/primary_35" />
    <corners android:radius="12dp" />
    <solid android:color="@android:color/transparent" />
</shape>
```

### File: `res/drawable/ic_flash_on.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/white">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M7,2v11h3v9l7,-12h-4l4,-8z" />
</vector>
```

### File: `res/drawable/ic_flash_off.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/white">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M16.12,11.5l1.88,-3.5h-4l1.54,-4H9.14l-0.38,0.71L7.6,3.56 8.56,2H18l-4,8h4l-2.03,3.57 -0.97,-1.07zM2.41,2L1,3.41l5.62,5.62L3,16h3v6l5.24,-7.78L17.59,21 19,19.59 2.41,2z" />
</vector>
```

---

## Bước 2.2: Layout — `activity_send_files.xml`

### File: `res/layout/activity_send_files.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/white">

    <!-- 1. Toolbar -->
    <LinearLayout
        android:id="@+id/layoutToolbar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_tool_bar"
        android:elevation="6dp"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp"
        android:paddingVertical="8dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <ImageView
            android:id="@+id/btnBack"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:contentDescription="@string/transfer_cancel"
            android:scaleType="centerInside"
            android:src="@drawable/ic_arrow_left" />

        <TextView
            android:id="@+id/tvTitle"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_marginStart="16dp"
            android:layout_marginEnd="16dp"
            android:layout_weight="1"
            android:fontFamily="@font/anton_regular"
            android:gravity="center_vertical"
            android:text="@string/send_files_title"
            android:textColor="#0A1207"
            android:textSize="22sp" />
    </LinearLayout>

    <!-- 2. TabLayout -->
    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabLayout"
        android:layout_width="0dp"
        android:layout_height="48dp"
        android:background="@null"
        android:elevation="4dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/layoutToolbar"
        app:tabGravity="start"
        app:tabIndicatorColor="@color/primary_35"
        app:tabIndicatorHeight="3dp"
        app:tabMode="scrollable"
        app:tabPaddingEnd="10dp"
        app:tabPaddingStart="10dp"
        app:tabSelectedTextColor="@color/primary_35"
        app:tabTextAppearance="@style/TextAppearance.AppCompat.Body2"
        app:tabTextColor="@color/sub_title" />

    <!-- 3. ViewPager2 -->
    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@id/layoutBottomBar"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tabLayout" />

    <!-- 4. Bottom Bar: selected count + Send button -->
    <LinearLayout
        android:id="@+id/layoutBottomBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_bottom_send_bar"
        android:elevation="8dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:paddingVertical="12dp"
        android:translationY="100dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <TextView
            android:id="@+id/tvSelectedCount"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:fontFamily="@font/outfit_medium"
            android:text="@string/send_files_selected"
            android:textColor="#0A1207"
            android:textSize="15sp" />

        <TextView
            android:id="@+id/btnSend"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_send_button"
            android:fontFamily="@font/outfit_semibold"
            android:paddingHorizontal="32dp"
            android:paddingVertical="12dp"
            android:text="@string/transfer_send"
            android:textColor="@color/white"
            android:textSize="15sp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## Bước 2.3: Layout — Fragment & Item Layouts

### File: `res/layout/fragment_send_recent.xml`

> Dùng chung cho Recent, File, Video, Music — chỉ 1 RecyclerView + empty state

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvFiles"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:paddingBottom="80dp" />

    <TextView
        android:id="@+id/tvEmpty"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:fontFamily="@font/outfit_medium"
        android:text="@string/send_no_files"
        android:textColor="@color/sub_title"
        android:textSize="15sp"
        android:visibility="gone" />

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

</FrameLayout>
```

### File: `res/layout/fragment_send_photo.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvPhotos"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:paddingBottom="80dp" />

    <TextView
        android:id="@+id/tvEmpty"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:fontFamily="@font/outfit_medium"
        android:text="@string/send_no_files"
        android:textColor="@color/sub_title"
        android:textSize="15sp"
        android:visibility="gone" />

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

</FrameLayout>
```

### File: `res/layout/item_send_file.xml`

> Dùng cho Recent, File, Video, Music

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="10dp"
    android:background="?attr/selectableItemBackground">

    <ImageView
        android:id="@+id/ivThumbnail"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:scaleType="centerCrop"
        android:src="@drawable/ic_file"
        tools:ignore="ContentDescription" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tvFileName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="middle"
            android:fontFamily="@font/outfit_medium"
            android:maxLines="1"
            android:textColor="#0A1207"
            android:textSize="14sp"
            tools:text="document.pdf" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvFileSize"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:fontFamily="@font/outfit_regular"
                android:textColor="@color/sub_title"
                android:textSize="12sp"
                tools:text="2.3 MB" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:text="•"
                android:textColor="@color/sub_title"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/tvFileDate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:fontFamily="@font/outfit_regular"
                android:textColor="@color/sub_title"
                android:textSize="12sp"
                tools:text="Sep 10, 2026" />
        </LinearLayout>
    </LinearLayout>

    <CheckBox
        android:id="@+id/cbSelect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:buttonTint="@color/primary_35"
        android:clickable="false"
        android:focusable="false" />

</LinearLayout>
```

### File: `res/layout/item_send_photo.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/ivPhoto"
        android:layout_width="match_parent"
        android:layout_height="120dp"
        android:scaleType="centerCrop" />

    <View
        android:id="@+id/viewOverlay"
        android:layout_width="match_parent"
        android:layout_height="120dp"
        android:background="#33000000"
        android:visibility="gone" />

    <CheckBox
        android:id="@+id/cbSelect"
        android:layout_width="28dp"
        android:layout_height="28dp"
        android:layout_gravity="top|end"
        android:layout_margin="4dp"
        android:background="@drawable/bg_photo_checkbox"
        android:button="@android:color/transparent"
        android:clickable="false"
        android:focusable="false" />

    <TextView
        android:id="@+id/tvDuration"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="4dp"
        android:background="#99000000"
        android:fontFamily="@font/outfit_medium"
        android:paddingHorizontal="4dp"
        android:paddingVertical="1dp"
        android:textColor="@color/white"
        android:textSize="10sp"
        android:visibility="gone" />

</FrameLayout>
```

### File: `res/layout/item_send_app.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="10dp"
    android:background="?attr/selectableItemBackground">

    <ImageView
        android:id="@+id/ivAppIcon"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:scaleType="centerCrop"
        tools:ignore="ContentDescription" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="12dp"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tvAppName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:fontFamily="@font/outfit_medium"
            android:maxLines="1"
            android:textColor="#0A1207"
            android:textSize="14sp"
            tools:text="Instagram" />

        <TextView
            android:id="@+id/tvAppSize"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:fontFamily="@font/outfit_regular"
            android:textColor="@color/sub_title"
            android:textSize="12sp"
            tools:text="156.3 MB" />
    </LinearLayout>

    <CheckBox
        android:id="@+id/cbSelect"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:buttonTint="@color/primary_35"
        android:clickable="false"
        android:focusable="false" />

</LinearLayout>
```

---

## Bước 2.4: Layout — QR Scanner

### File: `res/layout/activity_qr_scanner.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View
        android:id="@+id/viewOverlay"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="#66000000"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <View
        android:id="@+id/viewScanFrame"
        android:layout_width="250dp"
        android:layout_height="250dp"
        android:background="@drawable/bg_qr_scan_overlay"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.4" />

    <TextView
        android:id="@+id/tvHint"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:fontFamily="@font/outfit_medium"
        android:text="@string/qr_scanner_hint"
        android:textColor="@color/white"
        android:textSize="15sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/viewScanFrame" />

    <ImageView
        android:id="@+id/btnBack"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_marginStart="8dp"
        android:layout_marginTop="8dp"
        android:contentDescription="@string/transfer_cancel"
        android:padding="12dp"
        android:scaleType="centerInside"
        android:src="@drawable/ic_arrow_left"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:tint="@color/white" />

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:fontFamily="@font/outfit_semibold"
        android:text="@string/qr_scanner_title"
        android:textColor="@color/white"
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="@id/btnBack"
        app:layout_constraintEnd_toStartOf="@id/btnFlash"
        app:layout_constraintStart_toEndOf="@id/btnBack"
        app:layout_constraintTop_toTopOf="@id/btnBack" />

    <ImageView
        android:id="@+id/btnFlash"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_marginEnd="8dp"
        android:contentDescription="Flash"
        android:padding="12dp"
        android:scaleType="centerInside"
        android:src="@drawable/ic_flash_off"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/btnBack" />

    <LinearLayout
        android:id="@+id/layoutConnecting"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#CC000000"
        android:gravity="center"
        android:orientation="vertical"
        android:visibility="gone">

        <ProgressBar
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:indeterminateTint="@color/primary_35" />

        <TextView
            android:id="@+id/tvConnecting"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:fontFamily="@font/outfit_medium"
            android:text="@string/connection_searching"
            android:textColor="@color/white"
            android:textSize="16sp" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## Bước 2.5: SendFilesViewModel

### File: `ui/transfer/send/SendFilesViewModel.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send

import android.app.Application
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.util.transfer.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SharedViewModel cho SendFilesActivity
 * Quản lý:
 * - Danh sách file đã chọn (xuyên suốt tất cả tabs)
 * - Load dữ liệu cho từng tab từ MediaStore
 */
class SendFilesViewModel(application: Application) : AndroidViewModel(application) {

    // ===== SELECTED FILES (dùng chung cho tất cả tabs) =====

    private val _selectedFiles = MutableStateFlow<List<TransferFile>>(emptyList())
    val selectedFiles: StateFlow<List<TransferFile>> = _selectedFiles.asStateFlow()

    /**
     * Thêm hoặc bỏ chọn 1 file
     * Nếu đã chọn → bỏ, chưa chọn → thêm
     */
    fun toggleFileSelection(file: TransferFile) {
        val current = _selectedFiles.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.uri == file.uri }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
    }

    /**
     * Kiểm tra file đã được chọn chưa
     */
    fun isFileSelected(uri: Uri): Boolean {
        return _selectedFiles.value.any { it.uri == uri }
    }

    /**
     * Xóa toàn bộ selection
     */
    fun clearSelection() {
        _selectedFiles.value = emptyList()
    }

    /**
     * Lấy số lượng file đã chọn
     */
    fun getSelectedCount(): Int = _selectedFiles.value.size

    // ===== LOAD RECENT FILES =====

    data class FileItem(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String,
        val dateModified: Long
    )

    private val _recentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val recentFiles: StateFlow<List<FileItem>> = _recentFiles.asStateFlow()

    private val _isLoadingRecent = MutableStateFlow(false)
    val isLoadingRecent: StateFlow<Boolean> = _isLoadingRecent.asStateFlow()

    fun loadRecentFiles() {
        if (_recentFiles.value.isNotEmpty()) return // đã load rồi
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingRecent.value = true
            val files = queryRecentFiles()
            _recentFiles.value = files
            _isLoadingRecent.value = false
        }
    }

    private fun queryRecentFiles(): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val context = getApplication<Application>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0"

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            var count = 0
            while (cursor.moveToNext() && count < 200) { // giới hạn 200 file gần nhất
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "*/*"
                val dateModified = cursor.getLong(dateCol)

                val uri = MediaStore.Files.getContentUri("external", id)
                result.add(FileItem(uri, name, size, mimeType, dateModified))
                count++
            }
        }
        return result
    }

    // ===== LOAD INSTALLED APPS =====

    data class AppItem(
        val packageName: String,
        val appName: String,
        val apkPath: String,
        val size: Long,
        val icon: android.graphics.drawable.Drawable?
    )

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            val apps = queryInstalledApps()
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    private fun queryInstalledApps(): List<AppItem> {
        val context = getApplication<Application>()
        val pm = context.packageManager

        return pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // chỉ user apps
            .mapNotNull { appInfo ->
                val apkFile = File(appInfo.sourceDir)
                if (!apkFile.exists()) return@mapNotNull null

                AppItem(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    apkPath = appInfo.sourceDir,
                    size = apkFile.length(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    // ===== LOAD DOCUMENTS (File tab) =====

    private val _documentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val documentFiles: StateFlow<List<FileItem>> = _documentFiles.asStateFlow()

    private val _isLoadingDocs = MutableStateFlow(false)
    val isLoadingDocs: StateFlow<Boolean> = _isLoadingDocs.asStateFlow()

    fun loadDocuments() {
        if (_documentFiles.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingDocs.value = true
            val files = queryDocuments()
            _documentFiles.value = files
            _isLoadingDocs.value = false
        }
    }

    private fun queryDocuments(): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val context = getApplication<Application>()

        val docMimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv"
        )

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = docMimeTypes.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        }
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection, selection, docMimeTypes, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "*/*"
                val dateModified = cursor.getLong(dateCol)

                val uri = MediaStore.Files.getContentUri("external", id)
                result.add(FileItem(uri, name, size, mimeType, dateModified))
            }
        }
        return result
    }

    // ===== LOAD PHOTOS =====

    private val _photos = MutableStateFlow<List<FileItem>>(emptyList())
    val photos: StateFlow<List<FileItem>> = _photos.asStateFlow()

    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()

    fun loadPhotos() {
        if (_photos.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingPhotos.value = true
            val photoList = queryPhotos()
            _photos.value = photoList
            _isLoadingPhotos.value = false
        }
    }

    private fun queryPhotos(): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val context = getApplication<Application>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "image/*"
                val dateModified = cursor.getLong(dateCol)

                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()

                result.add(FileItem(uri, name, size, mimeType, dateModified))
            }
        }
        return result
    }

    // ===== LOAD VIDEOS =====

    data class VideoItem(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String,
        val dateModified: Long,
        val duration: Long // milliseconds
    )

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(false)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    fun loadVideos() {
        if (_videos.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingVideos.value = true
            val videoList = queryVideos()
            _videos.value = videoList
            _isLoadingVideos.value = false
        }
    }

    private fun queryVideos(): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        val context = getApplication<Application>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "video/*"
                val dateModified = cursor.getLong(dateCol)
                val duration = cursor.getLong(durationCol)

                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()

                result.add(VideoItem(uri, name, size, mimeType, dateModified, duration))
            }
        }
        return result
    }

    // ===== LOAD MUSIC =====

    data class MusicItem(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String,
        val dateModified: Long,
        val duration: Long,
        val artist: String
    )

    private val _musicFiles = MutableStateFlow<List<MusicItem>>(emptyList())
    val musicFiles: StateFlow<List<MusicItem>> = _musicFiles.asStateFlow()

    private val _isLoadingMusic = MutableStateFlow(false)
    val isLoadingMusic: StateFlow<Boolean> = _isLoadingMusic.asStateFlow()

    fun loadMusic() {
        if (_musicFiles.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMusic.value = true
            val music = queryMusic()
            _musicFiles.value = music
            _isLoadingMusic.value = false
        }
    }

    private fun queryMusic(): List<MusicItem> {
        val result = mutableListOf<MusicItem>()
        val context = getApplication<Application>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ARTIST
        )

        // Lọc bỏ notification sounds (duration < 30s)
        val selection = "${MediaStore.Audio.Media.DURATION} > 30000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val mimeType = cursor.getString(mimeCol) ?: "audio/*"
                val dateModified = cursor.getLong(dateCol)
                val duration = cursor.getLong(durationCol)
                val artist = cursor.getString(artistCol) ?: "Unknown"

                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build()

                result.add(MusicItem(uri, name, size, mimeType, dateModified, duration, artist))
            }
        }
        return result
    }

    // ===== UTILITIES =====

    /**
     * Format date từ timestamp (seconds) → "Sep 10, 2026"
     */
    fun formatDate(timestampSec: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        return sdf.format(Date(timestampSec * 1000))
    }

    /**
     * Format duration (ms) → "3:24"
     */
    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${sec.toString().padStart(2, '0')}"
    }
}
```

---

## Bước 2.6: SendTabPagerAdapter

### File: `ui/transfer/send/adapter/SendTabPagerAdapter.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.basekotlin.ui.transfer.send.fragment.FileFragment
import com.example.basekotlin.ui.transfer.send.fragment.InstalledAppsFragment
import com.example.basekotlin.ui.transfer.send.fragment.MusicFragment
import com.example.basekotlin.ui.transfer.send.fragment.PhotoFragment
import com.example.basekotlin.ui.transfer.send.fragment.RecentFragment
import com.example.basekotlin.ui.transfer.send.fragment.VideoFragment

/**
 * ViewPager2 adapter cho 6 tabs trong SendFilesActivity
 */
class SendTabPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_COUNT = 6
        const val TAB_RECENT = 0
        const val TAB_INSTALLED = 1
        const val TAB_FILE = 2
        const val TAB_PHOTO = 3
        const val TAB_VIDEO = 4
        const val TAB_MUSIC = 5
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_RECENT -> RecentFragment()
            TAB_INSTALLED -> InstalledAppsFragment()
            TAB_FILE -> FileFragment()
            TAB_PHOTO -> PhotoFragment()
            TAB_VIDEO -> VideoFragment()
            TAB_MUSIC -> MusicFragment()
            else -> RecentFragment()
        }
    }
}
```

---

## Bước 2.7: FileSelectionAdapter (dùng chung Recent/File/Video/Music)

### File: `ui/transfer/send/adapter/FileSelectionAdapter.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.basekotlin.R
import com.example.basekotlin.databinding.ItemSendFileBinding
import com.example.basekotlin.util.transfer.NetworkUtils

/**
 * Adapter cho danh sách file dạng list
 * Dùng chung cho tab: Recent, File (documents), Video, Music
 *
 * Hiển thị: thumbnail/icon + tên file + size + date + checkbox
 */
class FileSelectionAdapter(
    private val onItemClick: (FileSelectionItem) -> Unit
) : ListAdapter<FileSelectionItem, FileSelectionAdapter.FileViewHolder>(DiffCallback) {

    // Set lưu URI đã chọn, cập nhật từ ViewModel
    private val selectedUris = mutableSetOf<Uri>()

    /**
     * Cập nhật danh sách URI đã chọn
     * Gọi khi ViewModel.selectedFiles thay đổi
     */
    fun updateSelectedUris(uris: Set<Uri>) {
        val oldSelection = selectedUris.toSet()
        selectedUris.clear()
        selectedUris.addAll(uris)

        // Chỉ notify những item bị thay đổi selection
        currentList.forEachIndexed { index, item ->
            val wasSelected = oldSelection.contains(item.uri)
            val isSelected = selectedUris.contains(item.uri)
            if (wasSelected != isSelected) {
                notifyItemChanged(index, PAYLOAD_SELECTION)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemSendFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: FileViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            // Chỉ update checkbox, không rebind toàn bộ
            holder.updateSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class FileViewHolder(
        private val binding: ItemSendFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FileSelectionItem) {
            binding.tvFileName.text = item.name
            binding.tvFileSize.text = NetworkUtils.formatFileSize(item.size)
            binding.tvFileDate.text = item.dateFormatted

            // Load thumbnail dựa vào MIME type
            when {
                item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/") -> {
                    Glide.with(binding.ivThumbnail)
                        .load(item.uri)
                        .placeholder(R.drawable.ic_file)
                        .centerCrop()
                        .into(binding.ivThumbnail)
                }
                item.mimeType == "application/pdf" -> {
                    binding.ivThumbnail.setImageResource(R.drawable.ic_pdf)
                }
                item.mimeType.startsWith("audio/") -> {
                    binding.ivThumbnail.setImageResource(R.drawable.ic_music)
                }
                else -> {
                    binding.ivThumbnail.setImageResource(R.drawable.ic_file)
                }
            }

            updateSelection(item)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        fun updateSelection(item: FileSelectionItem) {
            binding.cbSelect.isChecked = selectedUris.contains(item.uri)
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection_changed"

        private val DiffCallback = object : DiffUtil.ItemCallback<FileSelectionItem>() {
            override fun areItemsTheSame(
                oldItem: FileSelectionItem,
                newItem: FileSelectionItem
            ): Boolean = oldItem.uri == newItem.uri

            override fun areContentsTheSame(
                oldItem: FileSelectionItem,
                newItem: FileSelectionItem
            ): Boolean = oldItem == newItem
        }
    }
}

/**
 * Data class cho item hiển thị trong list
 */
data class FileSelectionItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateFormatted: String,
    val extraInfo: String = "" // dùng cho duration, artist...
)
```

---

## Bước 2.8: PhotoSelectionAdapter

### File: `ui/transfer/send/adapter/PhotoSelectionAdapter.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.basekotlin.databinding.ItemSendPhotoBinding

/**
 * Grid adapter cho tab Photo
 * Hiển thị: thumbnail full + checkbox overlay
 */
class PhotoSelectionAdapter(
    private val onItemClick: (PhotoSelectionItem) -> Unit
) : ListAdapter<PhotoSelectionItem, PhotoSelectionAdapter.PhotoViewHolder>(DiffCallback) {

    private val selectedUris = mutableSetOf<Uri>()

    fun updateSelectedUris(uris: Set<Uri>) {
        val oldSelection = selectedUris.toSet()
        selectedUris.clear()
        selectedUris.addAll(uris)

        currentList.forEachIndexed { index, item ->
            val wasSelected = oldSelection.contains(item.uri)
            val isSelected = selectedUris.contains(item.uri)
            if (wasSelected != isSelected) {
                notifyItemChanged(index, PAYLOAD_SELECTION)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemSendPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: PhotoViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.updateSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class PhotoViewHolder(
        private val binding: ItemSendPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoSelectionItem) {
            Glide.with(binding.ivPhoto)
                .load(item.uri)
                .centerCrop()
                .into(binding.ivPhoto)

            binding.tvDuration.visibility = View.GONE

            updateSelection(item)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        fun updateSelection(item: PhotoSelectionItem) {
            val isSelected = selectedUris.contains(item.uri)
            binding.cbSelect.isChecked = isSelected
            binding.viewOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection_changed"

        private val DiffCallback = object : DiffUtil.ItemCallback<PhotoSelectionItem>() {
            override fun areItemsTheSame(
                oldItem: PhotoSelectionItem,
                newItem: PhotoSelectionItem
            ): Boolean = oldItem.uri == newItem.uri

            override fun areContentsTheSame(
                oldItem: PhotoSelectionItem,
                newItem: PhotoSelectionItem
            ): Boolean = oldItem == newItem
        }
    }
}

data class PhotoSelectionItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)
```

---

## Bước 2.9: AppSelectionAdapter

### File: `ui/transfer/send/adapter/AppSelectionAdapter.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.adapter

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.basekotlin.databinding.ItemSendAppBinding
import com.example.basekotlin.util.transfer.NetworkUtils

/**
 * Adapter cho tab Installed Apps
 * Hiển thị: app icon + tên app + size + checkbox
 */
class AppSelectionAdapter(
    private val onItemClick: (AppSelectionItem) -> Unit
) : ListAdapter<AppSelectionItem, AppSelectionAdapter.AppViewHolder>(DiffCallback) {

    private val selectedPackages = mutableSetOf<String>()

    fun updateSelectedPackages(packages: Set<String>) {
        val oldSelection = selectedPackages.toSet()
        selectedPackages.clear()
        selectedPackages.addAll(packages)

        currentList.forEachIndexed { index, item ->
            val wasSelected = oldSelection.contains(item.packageName)
            val isSelected = selectedPackages.contains(item.packageName)
            if (wasSelected != isSelected) {
                notifyItemChanged(index, PAYLOAD_SELECTION)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemSendAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: AppViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.updateSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class AppViewHolder(
        private val binding: ItemSendAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppSelectionItem) {
            binding.tvAppName.text = item.appName
            binding.tvAppSize.text = NetworkUtils.formatFileSize(item.size)
            item.icon?.let { binding.ivAppIcon.setImageDrawable(it) }

            updateSelection(item)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        fun updateSelection(item: AppSelectionItem) {
            binding.cbSelect.isChecked = selectedPackages.contains(item.packageName)
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection_changed"

        private val DiffCallback = object : DiffUtil.ItemCallback<AppSelectionItem>() {
            override fun areItemsTheSame(
                oldItem: AppSelectionItem,
                newItem: AppSelectionItem
            ): Boolean = oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(
                oldItem: AppSelectionItem,
                newItem: AppSelectionItem
            ): Boolean = oldItem.packageName == newItem.packageName
                    && oldItem.appName == newItem.appName
                    && oldItem.size == newItem.size
        }
    }
}

data class AppSelectionItem(
    val packageName: String,
    val appName: String,
    val apkPath: String,
    val size: Long,
    val icon: Drawable?,
    val uri: Uri // URI cho APK file
)
```

---

## Bước 2.10: SendFilesActivity

### File: `ui/transfer/send/SendFilesActivity.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.R
import com.example.basekotlin.databinding.ActivitySendFilesBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.scanner.QrScannerActivity
import com.example.basekotlin.ui.transfer.send.adapter.SendTabPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

/**
 * Màn hình chọn file để gửi
 * - 6 tabs: Recent | Installed | File | Photo | Video | Music
 * - Bottom bar hiển thị số file đã chọn + nút Send
 * - Nhấn Send → chuyển sang QrScannerActivity
 */
class SendFilesActivity : BaseActivity<ActivitySendFilesBinding>(
    ActivitySendFilesBinding::inflate
) {

    private val viewModel: SendFilesViewModel by viewModels()

    private val tabTitles by lazy {
        arrayOf(
            getString(R.string.send_tab_recent),
            getString(R.string.send_tab_installed),
            getString(R.string.send_tab_file),
            getString(R.string.send_tab_photo),
            getString(R.string.send_tab_video),
            getString(R.string.send_tab_music)
        )
    }

    // Permission request
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.loadRecentFiles()
        }
    }

    override fun initView() {
        setupViewPager()
        checkPermissions()
    }

    override fun bindView() {
        binding.btnBack.setOnClickListener { onBack() }

        binding.btnSend.setOnClickListener {
            navigateToQrScanner()
        }

        observeSelectedFiles()
    }

    private fun setupViewPager() {
        val pagerAdapter = SendTabPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = SendTabPagerAdapter.TAB_COUNT

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun observeSelectedFiles() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedFiles.collect { files ->
                    updateBottomBar(files)
                }
            }
        }
    }

    /**
     * Cập nhật bottom bar: hiện/ẩn với animation + số lượng file
     */
    private fun updateBottomBar(selectedFiles: List<TransferFile>) {
        if (selectedFiles.isNotEmpty()) {
            binding.tvSelectedCount.text =
                getString(R.string.send_files_selected, selectedFiles.size)

            if (binding.layoutBottomBar.visibility != View.VISIBLE) {
                binding.layoutBottomBar.visibility = View.VISIBLE
                binding.layoutBottomBar.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                binding.tvSelectedCount.text =
                    getString(R.string.send_files_selected, selectedFiles.size)
            }
        } else {
            binding.layoutBottomBar.animate()
                .translationY(binding.layoutBottomBar.height.toFloat())
                .setDuration(200)
                .withEndAction {
                    binding.layoutBottomBar.visibility = View.GONE
                }
                .start()
        }
    }

    /**
     * Chuyển sang QR Scanner, mang theo danh sách file đã chọn
     */
    private fun navigateToQrScanner() {
        val selectedFiles = viewModel.selectedFiles.value
        if (selectedFiles.isEmpty()) return

        val intent = Intent(this, QrScannerActivity::class.java).apply {
            putParcelableArrayListExtra(
                QrScannerActivity.EXTRA_FILES,
                ArrayList(selectedFiles)
            )
        }
        startActivity(intent)
        overridePendingTransition(R.anim.in_right, R.anim.out_left)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (permissions.isNotEmpty()) {
            storagePermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
```

---

## Bước 2.11: Fragments (6 tabs)

### File: `ui/transfer/send/fragment/RecentFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendRecentBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionItem
import kotlinx.coroutines.launch

/**
 * Tab Recent — hiển thị file mới nhất (tất cả loại)
 */
class RecentFragment : BaseFragment<FragmentSendRecentBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: FileSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendRecentBinding {
        return FragmentSendRecentBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = FileSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = item.name,
                size = item.size,
                mimeType = item.mimeType
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        viewModel.loadRecentFiles()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recentFiles.collect { files ->
                        val items = files.map { file ->
                            FileSelectionItem(
                                uri = file.uri,
                                name = file.name,
                                size = file.size,
                                mimeType = file.mimeType,
                                dateFormatted = viewModel.formatDate(file.dateModified)
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingRecent.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingRecent.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        adapter.updateSelectedUris(selectedFiles.map { it.uri }.toSet())
                    }
                }
            }
        }
    }
}
```

### File: `ui/transfer/send/fragment/InstalledAppsFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendRecentBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.AppSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.AppSelectionItem
import kotlinx.coroutines.launch
import java.io.File

/**
 * Tab Installed — hiển thị danh sách app đã cài
 */
class InstalledAppsFragment : BaseFragment<FragmentSendRecentBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: AppSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendRecentBinding {
        return FragmentSendRecentBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = AppSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = "${item.appName}.apk",
                size = item.size,
                mimeType = "application/vnd.android.package-archive"
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        viewModel.loadInstalledApps()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.installedApps.collect { apps ->
                        val items = apps.map { app ->
                            AppSelectionItem(
                                packageName = app.packageName,
                                appName = app.appName,
                                apkPath = app.apkPath,
                                size = app.size,
                                icon = app.icon,
                                uri = Uri.fromFile(File(app.apkPath))
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingApps.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingApps.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        val selectedPackages = selectedFiles
                            .filter { it.mimeType == "application/vnd.android.package-archive" }
                            .mapNotNull { file ->
                                adapter.currentList.find { it.uri == file.uri }?.packageName
                            }
                            .toSet()
                        adapter.updateSelectedPackages(selectedPackages)
                    }
                }
            }
        }
    }
}
```

### File: `ui/transfer/send/fragment/FileFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendRecentBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionItem
import kotlinx.coroutines.launch

/**
 * Tab File — hiển thị documents (PDF, DOC, XLS...)
 */
class FileFragment : BaseFragment<FragmentSendRecentBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: FileSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendRecentBinding {
        return FragmentSendRecentBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = FileSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = item.name,
                size = item.size,
                mimeType = item.mimeType
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        viewModel.loadDocuments()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.documentFiles.collect { files ->
                        val items = files.map { file ->
                            FileSelectionItem(
                                uri = file.uri,
                                name = file.name,
                                size = file.size,
                                mimeType = file.mimeType,
                                dateFormatted = viewModel.formatDate(file.dateModified)
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingDocs.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingDocs.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        adapter.updateSelectedUris(selectedFiles.map { it.uri }.toSet())
                    }
                }
            }
        }
    }
}
```

### File: `ui/transfer/send/fragment/PhotoFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendPhotoBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.PhotoSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.PhotoSelectionItem
import kotlinx.coroutines.launch

/**
 * Tab Photo — hiển thị ảnh dạng grid 3 cột
 */
class PhotoFragment : BaseFragment<FragmentSendPhotoBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: PhotoSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendPhotoBinding {
        return FragmentSendPhotoBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = PhotoSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = item.name,
                size = item.size,
                mimeType = item.mimeType
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvPhotos.adapter = adapter
        viewModel.loadPhotos()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.photos.collect { photos ->
                        val items = photos.map { photo ->
                            PhotoSelectionItem(
                                uri = photo.uri,
                                name = photo.name,
                                size = photo.size,
                                mimeType = photo.mimeType
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingPhotos.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingPhotos.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        adapter.updateSelectedUris(selectedFiles.map { it.uri }.toSet())
                    }
                }
            }
        }
    }
}
```

### File: `ui/transfer/send/fragment/VideoFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendRecentBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionItem
import kotlinx.coroutines.launch

/**
 * Tab Video — hiển thị danh sách video
 */
class VideoFragment : BaseFragment<FragmentSendRecentBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: FileSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendRecentBinding {
        return FragmentSendRecentBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = FileSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = item.name,
                size = item.size,
                mimeType = item.mimeType
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        viewModel.loadVideos()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.videos.collect { videos ->
                        val items = videos.map { video ->
                            FileSelectionItem(
                                uri = video.uri,
                                name = video.name,
                                size = video.size,
                                mimeType = video.mimeType,
                                dateFormatted = viewModel.formatDuration(video.duration),
                                extraInfo = viewModel.formatDuration(video.duration)
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingVideos.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingVideos.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        adapter.updateSelectedUris(selectedFiles.map { it.uri }.toSet())
                    }
                }
            }
        }
    }
}
```

### File: `ui/transfer/send/fragment/MusicFragment.kt`

```kotlin
package com.example.basekotlin.ui.transfer.send.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.base.BaseFragment
import com.example.basekotlin.databinding.FragmentSendRecentBinding
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.send.SendFilesViewModel
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionAdapter
import com.example.basekotlin.ui.transfer.send.adapter.FileSelectionItem
import kotlinx.coroutines.launch

/**
 * Tab Music — hiển thị danh sách nhạc
 */
class MusicFragment : BaseFragment<FragmentSendRecentBinding>() {

    private val viewModel: SendFilesViewModel by activityViewModels()
    private lateinit var adapter: FileSelectionAdapter

    override fun setBinding(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): FragmentSendRecentBinding {
        return FragmentSendRecentBinding.inflate(inflater!!, container, false)
    }

    override fun initView() {
        adapter = FileSelectionAdapter { item ->
            val transferFile = TransferFile(
                uri = item.uri,
                name = item.name,
                size = item.size,
                mimeType = item.mimeType
            )
            viewModel.toggleFileSelection(transferFile)
        }

        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
        viewModel.loadMusic()
    }

    override fun bindView() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.musicFiles.collect { musicList ->
                        val items = musicList.map { music ->
                            FileSelectionItem(
                                uri = music.uri,
                                name = music.name,
                                size = music.size,
                                mimeType = music.mimeType,
                                dateFormatted = music.artist,
                                extraInfo = viewModel.formatDuration(music.duration)
                            )
                        }
                        adapter.submitList(items)
                        binding.tvEmpty.visibility =
                            if (items.isEmpty() && !viewModel.isLoadingMusic.value) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoadingMusic.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.selectedFiles.collect { selectedFiles ->
                        adapter.updateSelectedUris(selectedFiles.map { it.uri }.toSet())
                    }
                }
            }
        }
    }
}
```

---

## Bước 2.12: QrScannerActivity

### File: `ui/transfer/scanner/QrScannerActivity.kt`

```kotlin
package com.example.basekotlin.ui.transfer.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.databinding.ActivityQrScannerBinding
import com.example.basekotlin.service.transfer.TransferService
import com.example.basekotlin.ui.transfer.model.ConnectionInfo
import com.example.basekotlin.ui.transfer.model.TransferFile
import com.example.basekotlin.ui.transfer.progress.TransferProgressActivity
import com.example.basekotlin.util.transfer.QrCodeHelper
import com.example.basekotlin.util.transfer.WifiHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Màn hình scan QR code từ receiver
 *
 * Flow:
 * 1. Nhận danh sách files từ SendFilesActivity
 * 2. Bật camera → scan QR code
 * 3. Parse QR → lấy ConnectionInfo (SSID, password, IP, port)
 * 4. Kết nối WiFi hotspot của receiver
 * 5. Start TransferService (send mode)
 * 6. Chuyển sang TransferProgressActivity
 */
class QrScannerActivity : BaseActivity<ActivityQrScannerBinding>(
    ActivityQrScannerBinding::inflate
) {

    companion object {
        private const val TAG = "QrScannerActivity"
        const val EXTRA_FILES = "EXTRA_FILES"
    }

    private var filesToSend: ArrayList<TransferFile> = arrayListOf()
    private var cameraControl: CameraControl? = null
    private var isFlashOn = false
    private var isQrProcessed = false // tránh scan trùng
    private lateinit var cameraExecutor: ExecutorService
    private var wifiHelper: WifiHelper? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.error_permission_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun getData() {
        filesToSend = intent.getParcelableArrayListExtra(EXTRA_FILES) ?: arrayListOf()
        if (filesToSend.isEmpty()) {
            finish()
            return
        }
    }

    override fun initView() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        wifiHelper = WifiHelper(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun bindView() {
        binding.btnBack.setOnClickListener { onBack() }

        binding.btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            cameraControl?.enableTorch(isFlashOn)
            binding.btnFlash.setImageResource(
                if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }
    }

    /**
     * Khởi tạo CameraX + ML Kit barcode scanner
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind thất bại", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Xử lý từng frame từ camera → detect QR code
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (isQrProcessed) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )

        val scanner = BarcodeScanning.getClient()
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT) {
                        val rawValue = barcode.rawValue ?: continue
                        val connectionInfo = QrCodeHelper.parseQrContent(rawValue)
                        if (connectionInfo != null) {
                            isQrProcessed = true
                            onQrCodeDetected(connectionInfo)
                            break
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning thất bại", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * QR code đã scan thành công → kết nối WiFi → start transfer
     */
    private fun onQrCodeDetected(connectionInfo: ConnectionInfo) {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

        runOnUiThread {
            Toast.makeText(this, R.string.qr_scan_success, Toast.LENGTH_SHORT).show()

            binding.layoutConnecting.visibility = View.VISIBLE
            binding.tvConnecting.text = getString(
                R.string.connection_connecting, connectionInfo.deviceName
            )

            connectToReceiver(connectionInfo)
        }
    }

    /**
     * Kết nối WiFi hotspot của receiver rồi start TransferService
     */
    private fun connectToReceiver(connectionInfo: ConnectionInfo) {
        wifiHelper?.connectToWifi(
            ssid = connectionInfo.ssid,
            password = connectionInfo.password,
            listener = object : WifiHelper.WifiConnectionListener {
                override fun onConnected(network: android.net.Network) {
                    Log.d(TAG, "Đã kết nối WiFi: ${connectionInfo.ssid}")
                    runOnUiThread {
                        // Start TransferService ở chế độ SEND
                        TransferService.startSend(
                            context = this@QrScannerActivity,
                            ip = connectionInfo.ipAddress,
                            port = connectionInfo.port,
                            files = filesToSend
                        )

                        // Chuyển sang TransferProgressActivity
                        // Lưu ý: TransferProgressActivity sẽ implement ở Phase 3
                        // Tạm thời có thể comment dòng dưới nếu chưa có class
                        val intent = Intent(
                            this@QrScannerActivity,
                            TransferProgressActivity::class.java
                        ).apply {
                            putExtra("DEVICE_NAME", connectionInfo.deviceName)
                            putExtra("IS_SENDER", true)
                        }
                        startActivity(intent)
                        overridePendingTransition(R.anim.in_right, R.anim.out_left)
                        finish()
                    }
                }

                override fun onDisconnected() {
                    runOnUiThread {
                        binding.layoutConnecting.visibility = View.GONE
                        isQrProcessed = false
                        Toast.makeText(
                            this@QrScannerActivity,
                            R.string.connection_failed_message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailed() {
                    runOnUiThread {
                        binding.layoutConnecting.visibility = View.GONE
                        isQrProcessed = false
                        Toast.makeText(
                            this@QrScannerActivity,
                            R.string.connection_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        wifiHelper?.disconnectWifi()
        super.onDestroy()
    }
}
```

---

## Bước 2.13: Đăng Ký Activity Trong AndroidManifest.xml

Thêm vào block `<application>` trong `AndroidManifest.xml`:

```xml
<activity
    android:name=".ui.transfer.send.SendFilesActivity"
    android:exported="false"
    android:screenOrientation="portrait"
    android:windowSoftInputMode="adjustPan" />

<activity
    android:name=".ui.transfer.scanner.QrScannerActivity"
    android:exported="false"
    android:screenOrientation="portrait" />
```

---

## Bước 2.14: Kết Nối Entry Point

Hiện tại `SendActivity` ở package `ui.send` rất basic. Có 2 lựa chọn:

### Phương án A: Navigate trực tiếp từ Home sang SendFilesActivity

```kotlin
// Trong MainActivity hoặc bất kỳ nơi gọi send:
startNextActivity(SendFilesActivity::class.java, null)
```

### Phương án B: Cập nhật SendActivity hiện có để chuyển tiếp

```kotlin
// Trong SendActivity.kt — khi nhấn btn_send trong toolbar_send.xml:
binding.layoutToolbar.btnSend.setOnClickListener {
    startActivity(Intent(this, SendFilesActivity::class.java))
    overridePendingTransition(R.anim.in_right, R.anim.out_left)
}
```

---

## Tóm Tắt Thứ Tự Implement

```
1. Tạo drawable resources (bg_send_button, bg_photo_checkbox, bg_qr_scan_overlay, etc.)
2. Tạo layout XML files (activity_send_files, fragment_send_recent, fragment_send_photo,
   item_send_file, item_send_photo, item_send_app, activity_qr_scanner)
3. Tạo SendFilesViewModel
4. Tạo SendTabPagerAdapter
5. Tạo 3 adapters (FileSelectionAdapter, PhotoSelectionAdapter, AppSelectionAdapter)
6. Tạo 6 fragments (Recent, InstalledApps, File, Photo, Video, Music)
7. Tạo SendFilesActivity
8. Tạo QrScannerActivity
9. Đăng ký Activities trong AndroidManifest
10. Kết nối entry point từ Home/SendActivity
```

---

## Lưu Ý Quan Trọng

### 1. Drawables có thể thiếu
Code tham chiếu các drawable có thể chưa có trong project:
- `ic_file` — icon file generic
- `ic_pdf` — icon PDF
- `ic_music` — icon nhạc
- Nếu thiếu → tạo vector drawable hoặc dùng Material Icons

### 2. Font
Code sử dụng: `outfit_regular`, `outfit_medium`, `outfit_semibold`, `anton_regular`
→ Đảm bảo đã có trong `res/font/`

### 3. TransferProgressActivity
`QrScannerActivity` navigate sang `TransferProgressActivity` — class này thuộc **Phase 3**.
→ Tạm tạo placeholder đơn giản hoặc comment dòng startActivity

### 4. Glide
Project đã có dependency Glide → adapter dùng Glide để load thumbnail

### 5. Permission
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` (API 33+)
- `CAMERA` cho QR Scanner
- minSdk = 29, targetSdk = 36

### 6. ViewBinding
Layout → binding classes tự generate:
- `activity_send_files.xml` → `ActivitySendFilesBinding`
- `fragment_send_recent.xml` → `FragmentSendRecentBinding`
- `fragment_send_photo.xml` → `FragmentSendPhotoBinding`
- `item_send_file.xml` → `ItemSendFileBinding`
- `item_send_photo.xml` → `ItemSendPhotoBinding`
- `item_send_app.xml` → `ItemSendAppBinding`
- `activity_qr_scanner.xml` → `ActivityQrScannerBinding`

---

## Cách Test Phase 2

### Test 1: Chọn file
```
1. Mở SendFilesActivity
2. Swipe qua 6 tabs → xem dữ liệu load đúng không
3. Tap chọn file → bottom bar hiện + counter đúng
4. Chuyển tab → file vẫn giữ selected
5. Bỏ chọn → counter giảm → khi = 0 → bottom bar ẩn
```

### Test 2: QR Scanner
```
1. Chọn file → nhấn Send → mở QrScannerActivity
2. Camera hiện đúng
3. Flash toggle hoạt động
4. Scan QR code test → parse thành ConnectionInfo
5. Hiện loading "Connecting..."
```

### Test 3: End-to-End (cần thiết bị thứ 2)
```
1. Thiết bị A: Mở ReceiveActivity (Phase 3)
2. Thiết bị B: Chọn file → Send → Scan QR từ A
3. Verify: WiFi kết nối → TransferService start → chuyển Progress
```
