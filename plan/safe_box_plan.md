# Tính năng Safe Box — Phân tích & Kế hoạch

## Tổng quan

Safe Box là một tính năng "két an toàn" trong app, cho phép user khoá riêng một vùng lưu trữ bằng Pattern Lock và di chuyển các file nhạy cảm (ảnh, video, audio, documents, others) vào đó. File trong SafeBox được ẩn khỏi gallery/media scanner thông thường.

---

## Phân tích Design từ ảnh

Thiết kế gồm **4 màn hình chính** và **2 trạng thái phụ**:

### 🔐 Màn hình 1 — SafeBox Entry (Pattern Lock)
| Trạng thái | Mô tả |
|---|---|
| **Lần đầu vào** (chưa có pattern) | Hiện icon khoá + "Draw your pattern to lock" + grid 3×3 (các node chưa active) |
| **Tạo pattern** | User vẽ pattern → các node sáng lên + đường nối màu xanh lá |
| **Confirm pattern** | Yêu cầu vẽ lại để xác nhận (màn hình riêng) |
| **Mở khoá** | Vẽ đúng pattern → vào màn hình Safe Box Home |
| **Sai pattern** | Text lỗi màu đỏ "Draw your pattern to unlock / Too many attempts" + nút "Redraw" |

### 📂 Màn hình 2 — Safe Box Home (danh sách loại file)
Các category hiển thị dạng grid 2 cột, mỗi item có icon màu + tên + số lượng file:
- 🟢 **Pictures** (màu xanh lá)
- 🔴 **Videos** (màu đỏ)
- 🟣 **Audio** (màu tím)
- 🔵 **Documents** (màu xanh dương)
- 🟩 **Others** (màu xanh đậm)
- FAB ở góc phải dưới (icon mũi tên xanh lá)

### 🖼️ Màn hình 3 — File List trong SafeBox (ví dụ: Pictures)
- Toolbar: nút Back + tên category + nút dấu +
- Danh sách file dạng list (thumbnail + tên + kích thước)
- Mỗi item có nút `⋮` (more menu)
- Khi list rỗng: hiển thị empty state (icon ? + "No Files Available")
- **Selection mode**: checkbox xuất hiện khi long press, nút **Add** (+) xuất hiện ở phần thêm phía dưới
- **More menu (3 chấm)** có 2 option: "Restore from SafeBox" và "Delete Permanently"
- **Bulk delete**: Dialog xác nhận "Delete File — Do you want to delete X selected files from this device? You won't be able to restore!" với nút Cancel + Delete (đỏ)

### 📁 Màn hình 4 — Others (danh sách file "khác")
- Hiển thị file với icon nhỏ theo loại (image, video, audio, doc, other)
- Layout tương tự Pictures/Videos

---

## Phân tích Codebase Hiện Tại

### ✅ Những gì đã có sẵn

| Thành phần | Vị trí | Ghi chú |
|---|---|---|
| Base classes | `base/BaseActivity`, `BaseFragment`, `BaseDialog` | Dùng lại hoàn toàn |
| Pattern tổ chức MVVM | Toàn bộ module (photos, video, docs…) | Cần theo sát |
| `move_to_safebox` string | `strings.xml:146` | Đã có |
| `moveToSafeBox()` stub | `PhotosActivity.kt:363` | TODO chờ implement |
| `tvMoveToSafebox` trong popup | `AllDocumentsFragment.kt:246` | Toast placeholder |
| `ConfirmActionDialog` | `dialog/common/` | Tái sử dụng cho dialog xoá |
| `TextInputDialog` | `dialog/common/` | Tái sử dụng |
| `PopupMenuUtils` | `util/` | Dùng cho more menu |
| Model classes (PhotoInfo, VideoInfo…) | `model/` | Có thể tái sử dụng |
| Glide, sdp/ssp, Room DB | `build.gradle` | Đã có sẵn |

### ❌ Những gì chưa có

- Pattern Lock View (custom view vẽ pattern 3×3)
- SafeBox data layer (lưu pattern hash, lưu danh sách file)
- SafeBox Activity/Fragment/ViewModel
- File move logic (copy → SafeBox dir → xoá khỏi media scanner)
- Restore logic (copy ngược lại → notify media scanner)
- Delete permanently logic

---

## Danh sách việc cần làm (TODO List)

### 🔧 LAYER 1 — Data & Storage

#### [NEW] `SafeBoxPreferences.kt`
- Lưu/đọc pattern hash (SHA-256) bằng `EncryptedSharedPreferences`
- Check `isPatternSet()`, `verifyPattern()`, `savePattern()`

#### [NEW] `SafeBoxDatabase` (Room)
- Entity: `SafeBoxFile(id, originalPath, safeboxPath, fileType, fileName, fileSize, dateAdded)`
- DAO: insert, delete, queryByType, queryAll

#### [NEW] `SafeBoxRepository.kt`
- `moveFileToSafeBox(originalPath, fileType)` — copy → ẩn khỏi MediaStore → lưu DB
- `restoreFileFromSafeBox(safeboxFile)` — copy lại → notify MediaStore → xoá DB
- `deletePermanently(safeboxFile)` — xoá file vật lý + xoá DB
- `getFilesByType(type)` — query DB theo loại

#### [NEW] `SafeBoxFileType.kt` (enum)
```kotlin
enum class SafeBoxFileType { PICTURES, VIDEOS, AUDIO, DOCUMENTS, OTHERS }
```

#### [NEW] `SafeBoxFile.kt` (data class / Room entity)

#### Cơ chế ẩn file:
- Di chuyển file vào thư mục riêng trong Internal Storage (không scan được bởi MediaStore)
- Tạo file `.nomedia` trong thư mục SafeBox để ngăn MediaStore index

---

### 🔐 LAYER 2 — Pattern Lock

#### [NEW] `PatternLockView.kt` (Custom View)
- Grid 3×3, mỗi node là circle
- Xử lý touch: khi drag qua node → activate + vẽ đường nối
- State: IDLE, DRAWING, SUCCESS, ERROR
- Emit pattern sequence qua callback

#### [NEW] `activity_safebox_lock.xml`
- Icon khoá + title text + PatternLockView + hint text + nút Redraw

#### [NEW] `SafeBoxLockActivity.kt`
- Mode: `SETUP_PATTERN` / `CONFIRM_PATTERN` / `UNLOCK`
- ViewModel xử lý logic state, gọi `SafeBoxPreferences`
- Khi UNLOCK thành công → mở `SafeBoxHomeActivity`

#### [NEW] `SafeBoxLockViewModel.kt`

---

### 🏠 LAYER 3 — Safe Box Home

#### [NEW] `SafeBoxHomeActivity.kt`
- Grid 2 cột hiển thị 5 category
- Khi click → mở `SafeBoxFileListActivity` với type tương ứng

#### [NEW] `activity_safebox_home.xml`
- RecyclerView grid + FAB

#### [NEW] `SafeBoxCategoryAdapter.kt`
- Item: icon + tên + số lượng file

#### [NEW] `SafeBoxHomeViewModel.kt`
- Đếm file theo từng type từ DB

---

### 📋 LAYER 4 — File List trong SafeBox

#### [NEW] `SafeBoxFileListActivity.kt`
- Nhận `SafeBoxFileType` qua Intent extra
- Toolbar: Back + title (theo type) + nút Add (+)
- List file + selection mode
- More menu: Restore / Delete Permanently
- Empty state khi không có file

#### [NEW] `activity_safebox_file_list.xml`

#### [NEW] `SafeBoxFileAdapter.kt`
- Item: thumbnail/icon + tên + size + nút more
- Selection mode: checkbox

#### [NEW] `SafeBoxFileListViewModel.kt`
- Load danh sách file theo type
- Toggle selection
- Restore / Delete logic

#### [NEW] `dialog_safebox_delete.xml`
- Dialog confirm xoá vĩnh viễn
- Text: "You won't be able to restore!"

---

### 🔗 LAYER 5 — Tích hợp vào các module hiện có

#### [MODIFY] `PhotosActivity.kt` — `moveToSafeBox(photo)`
- Implement gọi `SafeBoxRepository.moveFileToSafeBox()`
- Xoá ảnh khỏi MediaStore
- Refresh UI

#### [MODIFY] `AllDocumentsFragment.kt` — `tvMoveToSafebox`
- Thay Toast placeholder bằng logic thực

#### [MODIFY] Video module — thêm "Move to SafeBox" vào more menu
- Tương tự Photos/Documents

#### [MODIFY] Audio module — thêm "Move to SafeBox" vào more menu

#### [MODIFY] `AndroidManifest.xml`
- Đăng ký `SafeBoxLockActivity`, `SafeBoxHomeActivity`, `SafeBoxFileListActivity`

#### [MODIFY] `strings.xml`
- Thêm strings: `safe_box`, `draw_pattern_to_lock`, `draw_pattern_to_unlock`, `create_lock_pattern`, `confirm_lock_pattern`, `pattern_incorrect`, `too_many_attempts`, `redraw`, `pictures`, `videos`, `audio`, `others`, `no_files_available`, `no_files_available_desc`, `restore_from_safebox`, `delete_permanently`, `delete_file_safebox_title`, `delete_file_safebox_desc`, `move_to_safebox_success`, `restore_success`

---

## Thứ tự Implement Đề Xuất

```
1. SafeBoxFile entity + DAO + Database (Room)
2. SafeBoxPreferences (EncryptedSharedPreferences)
3. SafeBoxRepository
4. PatternLockView (Custom View)
5. SafeBoxLockActivity + ViewModel + Layout
6. SafeBoxHomeActivity + ViewModel + Layout
7. SafeBoxFileListActivity + ViewModel + Layout + Adapter
8. Tích hợp vào Photos, Videos, Audio, Documents
9. Strings + Manifest
```

---

## Lưu ý kỹ thuật

> [!IMPORTANT]
> - Pattern phải được hash trước khi lưu (SHA-256), **không lưu raw pattern**
> - Dùng `EncryptedSharedPreferences` cho bảo mật
> - File SafeBox lưu ở `context.filesDir/safebox/` (Internal Storage, không scan được)
> - Tạo file `.nomedia` để ngăn MediaStore index thư mục

> [!WARNING]
> - Cần cẩn thận khi xoá khỏi MediaStore (`ContentResolver.delete()`) — một số thiết bị Android 10+ cần `createDeleteRequest()`
> - Thư mục SafeBox nằm trong Internal Storage → không cần permission READ/WRITE_EXTERNAL_STORAGE để đọc/ghi

> [!NOTE]
> - Custom `PatternLockView` viết từ đầu (không dùng thư viện ngoài) để tránh phụ thuộc không cần thiết
> - Design theme: màu xanh lá chủ đạo (#4CAF50 hoặc tương đương), background gradient trắng-xanh nhạt như các màn hình khác trong app
