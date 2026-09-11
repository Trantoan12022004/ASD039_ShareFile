# Phân tích tính năng màn Clean File

Dựa trên screenshot và codebase hiện tại, dưới đây là danh sách các tính năng cần phát triển:

## Tổng quan màn hình

Màn Clean File gồm 3 khu vực chính:

| Khu vực | Mô tả |
|---------|-------|
| **Header** | Hiển thị tổng dung lượng junk files + nút "Clean Up More" |
| **System Cleaners** | Grid 6 tính năng dọn dẹp hệ thống |
| **Messenger Cleaners** | Danh sách dọn dẹp media từ ứng dụng nhắn tin |

---

## Trạng thái phát triển

### ✅ Đã có (một phần)
| Tính năng | File hiện tại | Ghi chú |
|-----------|---------------|---------|
| Layout Junk Memory card | [layout_junk_memory.xml](file:///d:/mobile/ASD039/app/src/main/res/layout/layout_junk_memory.xml) | Chỉ có layout, chưa có logic quét/xóa |
| Nút "Clean" cache | [activity_files.xml](file:///d:/mobile/ASD039/app/src/main/res/layout/activity_files.xml#L382-L444) | Có UI trong màn Files, chưa có logic thực thi |

---

### 🔴 Chưa phát triển — Cần làm mới hoàn toàn

#### 1. Màn Clean File chính (Activity/Fragment)
> Hiện **CHƯA có** Activity/Fragment nào cho màn Clean File trong screenshot. Cần tạo:
- `CleanFileActivity` hoặc `CleanFileFragment`
- Layout `activity_clean_file.xml`
- ViewModel `CleanFileViewModel`

#### 2. Header — Junk Scanner
- Quét tổng dung lượng junk files (cache, temp, residual files)
- Hiển thị dung lượng "305,5 KB Junk files found"
- Animation icon thùng rác
- Nút **"Clean Up More"** → điều hướng tới quét chi tiết phone cache

#### 3. System Cleaners (6 tính năng)

| # | Tính năng | Mô tả | Trạng thái |
|---|-----------|-------|------------|
| 1 | **Apps Cleanup** | Quét & liệt kê app, apk | 🔴 Chưa có |
| 2 | **Big Files** | Quét file lớn (>500MB ), cho phép xóa | 🔴 Chưa có |
| 3 | **Video Cleanup** | Quét video không cần thiết, video trùng lặp, video quá lớn | 🔴 Chưa có |
| 4 | **Photo Cleanup** | Quét ảnh tương tự, ảnh mờ, screenshot cũ | 🔴 Chưa có |
| 5 | **Audio Cleanup** | Quét audio không sử dụng, file recording cũ | 🔴 Chưa có |
| 6 | **Duplicate Files** | Quét file trùng lặp dựa trên hash/tên/kích thước | 🔴 Chưa có |

#### 4. Messenger Cleaners (2 tính năng)

| # | Tính năng | Mô tả | Trạng thái |
|---|-----------|-------|------------|
| 1 | **Telegram Cleaner** | Quét thư mục Telegram, liệt kê media (ảnh, video, voice) đã tải | 🔴 Chưa có |
| 2 | **Whatsapp Cleaner** | Quét thư mục WhatsApp, liệt kê media chat | 🔴 Chưa có |

---

## Chi tiết kỹ thuật cần phát triển cho từng tính năng

### 🔧 1. Apps Cleanup

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Apps Cleanup"
- **2 Tabs**: `Installed Apps` | `APK Package`
- **Header**: Hiển thị tổng dung lượng (ví dụ: "Installed (21,6 GB)" hoặc "APK Package (21,6 GB)")
- **Danh sách**: RecyclerView hiển thị list app/APK

---

#### Tab 1: Installed Apps

**Item trong danh sách:**
| Thành phần | Mô tả |
|------------|-------|
| App Icon | Icon ứng dụng đã cài, hình tròn |
| App Name | Tên ứng dụng (ví dụ: "App", "Zipppp") |
| Size | Dung lượng app (ví dụ: "12,1 MB") |
| Description | Thông tin thời gian (ví dụ: "Includes for 17 days") |
| More button | Nút 3 chấm dọc (⋮) bên phải |

**Popup Menu (nhấn nút ⋮):**
| Action | Mô tả |
|--------|-------|
| Open | Mở ứng dụng |
| Uninstall | Hiển thị dialog xác nhận gỡ cài đặt |


**Dialog "Uninstall App":**
- Title: "Uninstall App"
- Message: "Do you want to uninstall this app from this device?"
- Buttons: `Cancel` (outline) | `Uninstall` (đỏ/hồng, filled)

---

#### Tab 2: APK Package

**Item trong danh sách:**
| Thành phần | Mô tả |
|------------|-------|
| APK Icon | Icon/thumbnail của file APK |
| App Name | Tên APK (ví dụ: "Apppp") |
| Size | Dung lượng file (ví dụ: "12,1 MB") |
| Description | Thông tin thời gian (ví dụ: "Includes for 17 days") |
| Checkbox | Checkbox bên phải để multi-select |
| More button | Nút 3 chấm dọc (⋮) |

**Popup Menu (nhấn nút ⋮):**
| Action | Mô tả |
|--------|-------|
| Send | Gửi file APK |
| Share | Chia sẻ APK |
| Delete | Hiển thị dialog xác nhận xóa |
| Information | Hiển thị dialog chi tiết thông tin |


**Dialog "Delete APK":**
- Title: "Delete APK"
- Message: "Do you want to delete selected APK from this device?"
- Buttons: `Cancel` (outline) | `Delete` (xanh lá, filled)

**Dialog "Detail Information":**
- Title: "Detail Information"
- Các trường thông tin:
  - **Name**: Tên file APK (ví dụ: "Modern plan")
  - **Path**: Đường dẫn (ví dụ: "storage/emulated/0/Download/...")
  - **Size**: Dung lượng (ví dụ: "3,3 MB")
- Button: `Got It` (xanh lá, full-width, filled)

**Empty State (khi không có APK):**
- Icon: Grid icon ở giữa màn hình
- Title: "No APK To Clean Up"
- Subtitle: "You don't have to do anything"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `AppsCleanupActivity` |
| Layout | `activity_apps_cleanup.xml` |
| ViewModel | `AppsCleanupViewModel` |
| Tab Adapter | `AppsCleanupPagerAdapter` (ViewPager2 + TabLayout) |
| Fragment 1 | `InstalledAppsFragment` + `fragment_installed_apps.xml` |
| Fragment 2 | `ApkPackageFragment` + `fragment_apk_package.xml` |
| Adapter 1 | `InstalledAppsAdapter` + `item_installed_app.xml` |
| Adapter 2 | `ApkPackageAdapter` + `item_apk_package.xml` |
| Popup 1 | `popup_installed_app.xml` (Open / Uninstall / Share) |
| Popup 2 | `popup_apk_package.xml` (Send / Delete / Information) |
| Dialog 1 | `UninstallAppDialog` |
| Dialog 2 | `DeleteApkDialog` |
| Dialog 3 | `DetailInformationDialog` |

### 🔧 2. Big Files

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Big Files" + nút `Select All` (checkbox) bên phải
- **5 Filter Tabs**: `All` | `Photo` | `Video` | `Music` | `Other`
- **Header**: Hiển thị tổng dung lượng (ví dụ: "Installed (21,6 GB)")
- **Danh sách**: RecyclerView hiển thị list file lớn
- **Bottom Button**: Nút `Delete (n)` cố định ở dưới cùng, màu đỏ đậm, hiển thị số file đã chọn

---

#### Danh sách file

**Item trong danh sách:**
| Thành phần | Mô tả |
|------------|-------|
| File Icon | Icon/thumbnail file, hình tròn |
| File Name | Tên file (ví dụ: "App", "Zipppp") |
| Size | Dung lượng file (ví dụ: "51,1 KB") |
| Description | Thông tin thời gian (ví dụ: "Includes for 17 days") |
| Checkbox | Checkbox bên phải để chọn file (tick xanh lá khi được chọn) |

#### Chức năng chọn
- **Select All**: Checkbox trên toolbar, chọn/bỏ chọn tất cả file
- **Multi-select**: Tick từng file riêng lẻ qua checkbox
- **Nút Delete (n)**: Cố định bottom, hiển thị số lượng file đã chọn (ví dụ: "Delete (2)")

---

#### Dialog "Delete Files"
- Title: "Delete Files"
- Message: "Do you want to delete 2 selected files from this device?"
- Buttons: `Cancel` (outline) | `Delete` (đỏ, filled)

---

#### Empty State (khi không có file lớn)
- Icon: Dấu chấm hỏi (?) ở giữa màn hình
- Title: "No Files To Cleanup"
- Subtitle: "There is no file to be cleaned"
- Nút Delete vẫn hiển thị ở bottom (disabled hoặc ẩn)

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `BigFilesActivity` |
| Layout | `activity_big_files.xml` |
| ViewModel | `BigFilesViewModel` |
| Adapter | `BigFilesAdapter` + `item_big_file.xml` |
| Dialog | `DeleteFilesDialog` |
| Filter logic | Lọc theo category: All / Photo / Video / Music / Other |

### 🔧 3. Video Cleanup

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Video Cleanup" + nút `Select All` (checkbox) bên phải
- **Search bar**: Ô tìm kiếm "Search..." ngay dưới toolbar
- **Danh sách**: RecyclerView hiển thị video **nhóm theo folder** (expandable sections)
- **Bottom Button**: Nút `Delete (n)` cố định ở dưới cùng, màu đỏ đậm

---

#### Danh sách video (nhóm theo folder)

**Section header (expandable):**
| Thành phần | Mô tả |
|------------|-------|
| Folder Name | Tên thư mục (ví dụ: "Facebook", "Hehe", "Outlook") |
| Count | Số lượng video trong folder (ví dụ: "(3)", "(1)") |
| Arrow | Mũi tên ^ để expand/collapse danh sách |

**Item video trong từng section:**
| Thành phần | Mô tả |
|------------|-------|
| Thumbnail | Ảnh thumbnail của video |
| File Name | Tên file video (ví dụ: "VID_20200910_102639_BAN_252") |
| Size | Dung lượng video |
| Checkbox | Checkbox bên phải để chọn (tick xanh lá khi được chọn) |

#### Chức năng
- **Search**: Tìm kiếm video theo tên file
- **Select All**: Checkbox trên toolbar, chọn/bỏ chọn tất cả video
- **Multi-select**: Tick từng video riêng lẻ qua checkbox
- **Expand/Collapse**: Nhấn vào header folder để mở rộng/thu gọn danh sách video bên trong

---

#### Dialog "Delete Files"
- Title: "Delete Files"
- Message: "Do you want to delete n selected files from this device?"
- Buttons: `Cancel` (outline) | `Delete` (đỏ, filled)

---

#### Empty State (khi không có video)
- Icon: Icon video play (▶) ở giữa màn hình
- Title: "No Files To Cleanup"
- Subtitle: "There is no file to be cleaned"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `VideoCleanupActivity` |
| Layout | `activity_video_cleanup.xml` |
| ViewModel | `VideoCleanupViewModel` |
| Adapter | `VideoCleanupAdapter` (ExpandableRecyclerView hoặc section-based) |
| Item layouts | `item_video_cleanup_header.xml` + `item_video_cleanup.xml` |
| Dialog | `DeleteFilesDialog` (tái sử dụng từ Big Files) |

### 🔧 4. Photo Cleanup

> Cấu trúc tương tự **Video Cleanup**, chỉ khác loại media là ảnh.

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Photo Cleanup" + nút `Select All` (checkbox) bên phải
- **Search bar**: Ô tìm kiếm "Search..." ngay dưới toolbar
- **Danh sách**: RecyclerView hiển thị ảnh **nhóm theo folder** (expandable sections)
- **Bottom Button**: Nút `Delete (n)` cố định ở dưới cùng, màu đỏ đậm

---

#### Danh sách ảnh (nhóm theo folder)

**Section header (expandable):**
| Thành phần | Mô tả |
|------------|-------|
| Folder Name | Tên thư mục (ví dụ: "Camera", "Screenshots", "Facebook") |
| Count | Số lượng ảnh trong folder |
| Arrow | Mũi tên ^ để expand/collapse |

**Item ảnh trong từng section:**
| Thành phần | Mô tả |
|------------|-------|
| Thumbnail | Ảnh thumbnail |
| File Name | Tên file ảnh |
| Size | Dung lượng ảnh |
| Checkbox | Checkbox bên phải (tick xanh lá khi được chọn) |

---

#### Dialog "Delete Files"
- Tái sử dụng `DeleteFilesDialog` từ Big Files / Video Cleanup

#### Empty State
- Icon: Icon ảnh (🖼) ở giữa màn hình
- Title: "No Files To Cleanup"
- Subtitle: "There is no file to be cleaned"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `PhotoCleanupActivity` |
| Layout | `activity_photo_cleanup.xml` |
| ViewModel | `PhotoCleanupViewModel` |
| Adapter | `PhotoCleanupAdapter` (section-based, tương tự VideoCleanupAdapter) |
| Item layouts | `item_photo_cleanup_header.xml` + `item_photo_cleanup.xml` |
| Dialog | `DeleteFilesDialog` (tái sử dụng) |

### 🔧 5. Audio Cleanup

> Cấu trúc tương tự **Video Cleanup**, chỉ khác loại media là audio.

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Audio Cleanup" + nút `Select All` (checkbox) bên phải
- **Search bar**: Ô tìm kiếm "Search..." ngay dưới toolbar
- **Danh sách**: RecyclerView hiển thị audio **nhóm theo folder** (expandable sections)
- **Bottom Button**: Nút `Delete (n)` cố định ở dưới cùng, màu đỏ đậm

---

#### Danh sách audio (nhóm theo folder)

**Section header (expandable):**
| Thành phần | Mô tả |
|------------|-------|
| Folder Name | Tên thư mục (ví dụ: "Recordings", "Music", "WhatsApp Audio") |
| Count | Số lượng audio trong folder |
| Arrow | Mũi tên ^ để expand/collapse |

**Item audio trong từng section:**
| Thành phần | Mô tả |
|------------|-------|
| Thumbnail | Icon audio / album art |
| File Name | Tên file audio |
| Size | Dung lượng audio |
| Checkbox | Checkbox bên phải (tick xanh lá khi được chọn) |

---

#### Dialog "Delete Files"
- Tái sử dụng `DeleteFilesDialog` từ Big Files / Video Cleanup

#### Empty State
- Icon: Icon audio (🎵) ở giữa màn hình
- Title: "No Files To Cleanup"
- Subtitle: "There is no file to be cleaned"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `AudioCleanupActivity` |
| Layout | `activity_audio_cleanup.xml` |
| ViewModel | `AudioCleanupViewModel` |
| Adapter | `AudioCleanupAdapter` (section-based, tương tự VideoCleanupAdapter) |
| Item layouts | `item_audio_cleanup_header.xml` + `item_audio_cleanup.xml` |
| Dialog | `DeleteFilesDialog` (tái sử dụng) |

### 🔧 6. Duplicate Files

#### Cấu trúc màn hình
- **Toolbar**: Nút Back + Title "Duplicate Files" + nút `Select All` (checkbox) bên phải
- **Danh sách**: RecyclerView hiển thị file trùng lặp **nhóm theo duplicate group** (expandable sections)
- **Bottom Button**: Nút `Delete (n)` cố định ở dưới cùng, màu đỏ đậm

---

#### Danh sách file trùng lặp (nhóm theo group)

**Section header (expandable):**
| Thành phần | Mô tả |
|------------|-------|
| Group Name | Tên nhóm trùng lặp (ví dụ: "Group 1", "Group 2", "Group 3", "Downloaded") |
| Total Size | Tổng dung lượng nhóm (ví dụ: "(13,2 MB)", "(15,2 MB)") |
| Arrow | Mũi tên ^ để expand/collapse |

**Item file trong từng group:**
| Thành phần | Mô tả |
|------------|-------|
| Thumbnail | Ảnh thumbnail / icon file |
| File Name | Tên file (ví dụ: "Pretty When You Cry") |
| Size | Dung lượng file |
| Checkbox | Checkbox bên phải (tick xanh lá khi được chọn) |

#### Chức năng
- **Select All**: Checkbox trên toolbar, chọn/bỏ chọn tất cả file
- **Multi-select**: Tick từng file riêng lẻ qua checkbox
- **Nút Delete (n)**: Cố định bottom, hiển thị số lượng file đã chọn (disabled khi chưa chọn)

---

#### Dialog "Delete Files"
- Title: "Delete Files"
- Message: "Do you want to delete n selected files from this device?"
- Buttons: `Cancel` (outline) | `Delete` (đỏ, filled)

---

#### Empty State (khi không có file trùng lặp)
- Header: Hiển thị tổng dung lượng (ví dụ: "Installed (21,8 GB)")
- Icon: Dấu chấm hỏi (?) ở giữa màn hình
- Title: "No Duplicate Files To Cleanup"
- Subtitle: "There is no duplicate file to be cleaned"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `DuplicateFilesActivity` |
| Layout | `activity_duplicate_files.xml` |
| ViewModel | `DuplicateFilesViewModel` |
| Adapter | `DuplicateFilesAdapter` (section-based, tương tự VideoCleanupAdapter) |
| Item layouts | `item_duplicate_group_header.xml` + `item_duplicate_file.xml` |
| Dialog | `DeleteFilesDialog` (tái sử dụng) |
| Logic | Quét file trùng lặp dựa trên hash/tên/kích thước, gom nhóm |

### 🔧 7. Telegram Cleaner

#### Cấu trúc màn hình chính
- **Header**: Nền xanh dương (blue), chiếm phần trên màn hình
  - Nút Back + Title "Telegram File Cleaner"
  - Hiển thị tổng dung lượng lớn ở giữa (ví dụ: "0 B")
  - Subtitle: "Manage phone storage"
- **Danh sách categories**: Nền trắng, phía dưới header

---

#### Danh sách categories

| # | Category | Icon | Mô tả | Bên phải |
|---|----------|------|-------|----------|
| 1 | **Junk Files** | 🟢 (xanh lá) | "Rest assured. These are junks." | Badge "No Junk Found" (text xanh lá) |
| 2 | **Photos** | 🟢 (xanh lá) | "Clean up photos in chats" | Dung lượng (ví dụ: "0B") + mũi tên > |
| 3 | **Videos** | 🟣 (tím) | "Clean up videos in chats" | Dung lượng + mũi tên > |
| 4 | **Audios** | 🔴 (đỏ) | "Clean up audios in chats" | Dung lượng + mũi tên > |
| 5 | **Files** | 🟣 (tím) | "Clean up files in chats" | Dung lượng + mũi tên > |

---

#### Màn detail khi nhấn vào category (Photos / Videos / Audios / Files)
- **Toolbar**: Nút X (close) + Title category (ví dụ: "Photos")
- **Danh sách file**: Hiển thị các file media thuộc category đó
- **Bottom**: Nút `Delete` (icon thùng rác + text "Delete")

#### Empty State (trong màn detail)
- Illustration: Hình minh họa người cầm điện thoại
- Text: "No photo" / "No video" / "No audio" / "No file"

---

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `TelegramCleanerActivity` |
| Layout | `activity_telegram_cleaner.xml` |
| ViewModel | `TelegramCleanerViewModel` |
| Adapter | `CleanerCategoryAdapter` + `item_cleaner_category.xml` |
| Detail Activity | `CleanerDetailActivity` (dùng chung cho Photos/Videos/Audios/Files) |
| Detail Layout | `activity_cleaner_detail.xml` |
| Detail Adapter | `CleanerDetailAdapter` + `item_cleaner_detail.xml` |

### 🔧 8. Whatsapp Cleaner

> Cấu trúc **tương tự Telegram Cleaner**, chỉ khác:
> - Title: "WhatsApp File Cleaner"
> - Quét thư mục WhatsApp thay vì Telegram

#### Tóm tắt các thành phần cần tạo

| Loại | File cần tạo |
|------|-------------|
| Activity | `WhatsappCleanerActivity` |
| Layout | Tái sử dụng `activity_telegram_cleaner.xml` hoặc tạo `activity_whatsapp_cleaner.xml` |
| ViewModel | `WhatsappCleanerViewModel` |
| Detail | Tái sử dụng `CleanerDetailActivity` |

> **Lưu ý**: Telegram Cleaner và WhatsApp Cleaner có thể dùng chung base class / layout vì cấu trúc giống nhau, chỉ khác nguồn dữ liệu (thư mục quét).

---

## Thứ tự ưu tiên đề xuất

| Ưu tiên | Tính năng | Lý do |
|---------|-----------|-------|
| 🥇 P0 | Màn Clean File chính + Header | Khung cơ bản, entry point |
| 🥇 P0 | Apps Cleanup | Core feature, giải phóng cache nhiều nhất |
| 🥈 P1 | Big Files | Dễ implement, impact lớn |
| 🥈 P1 | Duplicate Files | Giá trị cao cho user |
| 🥉 P2 | Photo Cleanup | Phức tạp hơn (cần image hashing) |
| 🥉 P2 | Video Cleanup | Tương tự Photo |
| 🥉 P2 | Audio Cleanup | Ít phức tạp |
| 🔵 P3 | Telegram Cleaner | Phụ thuộc user có cài Telegram |
| 🔵 P3 | Whatsapp Cleaner | Phụ thuộc user có cài WhatsApp |

> [!IMPORTANT]
> Tất cả 8 tính năng trong screenshot đều **CHƯA được phát triển** trong codebase hiện tại. Chỉ có layout `layout_junk_memory.xml` và phần clean cache trong `activity_files.xml` là có sẵn nhưng chưa có logic xử lý.
