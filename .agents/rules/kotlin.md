---
trigger: always_on
---

---
description: Recommended Kotlin coding style
globs:
- "**/*.kt"
alwaysApply: true
---

Write Kotlin code following official Kotlin and Android best practices.

Requirements:

- Follow official Kotlin coding conventions and Android best practices.
- Prioritize readability, maintainability, and idiomatic Kotlin.
- Use expression bodies when they improve readability.
- Use smart casts and null-safety features appropriately.
- Prefer `when` over multiple chained `if/else` statements when appropriate.
- Use scope functions (`let`, `apply`, `run`, `also`, `with`) only when they improve readability.
- Avoid deeply nested scope functions.
- Prefer immutable variables (`val`) by default.
- Use `var` only when reassignment is necessary.
- Use meaningful and descriptive variable and function names.
- Keep functions small and focused on a single responsibility.
- Avoid unnecessary temporary variables.
- Avoid overly complex one-line expressions.
- Prefer clear and idiomatic Kotlin over Java-style Kotlin.
- Use early returns when they improve readability and reduce nesting.
- Follow Kotlin null-safety practices and avoid unnecessary `!!`.
- Do not use `hashCode()` on a String unless explicitly required.
- Add comments only when they explain non-obvious logic.
- Do not comment obvious code.
- Optimize for readability, maintainability, and idiomatic Kotlin.

## Android SDK Version Rules

- Before adding any `Build.VERSION.SDK_INT` check, always verify the project's `minSdk`.
- Never write an SDK version condition that is impossible based on `minSdk`.

Examples:
- If `minSdk >= 29`, do NOT write:
  `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)`
  because the condition is always true.
- If `minSdk >= 29`, do NOT write checks such as:
  `Build.VERSION.SDK_INT < 29`
  because they are always false.

- Remove unnecessary compatibility branches for Android versions below `minSdk`.
- Only use `Build.VERSION.SDK_INT` checks when the API level condition can actually vary on supported devices.

## Code Cleanliness Rules

- Do not use redundant qualifier names.
- If a class, object, or member can be referenced directly without ambiguity, prefer the shorter direct reference.
- Avoid unnecessary fully-qualified names such as:
  `android.os.Build.VERSION.SDK_INT`
  when `Build.VERSION.SDK_INT` is sufficient.
- Remove redundant imports, qualifiers, conditions, and compatibility code.

## Android Performance & Architecture Rules

### 1. RecyclerView Selection & Payloads Optimization
- KHÔNG BAO GIỜ gọi `notifyDataSetChanged()` khi người dùng chỉ chọn/bỏ chọn một item (việc này khiến toàn bộ danh sách bị re-bind, hủy và nạp lại coroutine/ảnh gây giật lag/jank UI).
- BẮT BUỘC sử dụng `notifyItemChanged(position, PAYLOAD)` kết hợp xử lý payload trong `onBindViewHolder(holder, position, payloads)` để chỉ cập nhật riêng checkbox / trạng thái chọn của item đó mà không re-render lại toàn bộ view hay nạp lại icon.
- Sử dụng `Set<String>` (ví dụ: `selectedPaths`, `selectedIds`) để kiểm tra trạng thái chọn với độ phức tạp O(1) tức thì. Tránh dùng `List.any` hoặc lọc lại toàn bộ `listData` trên luồng chính khi click chọn.

### 2. Fast File Querying (No Recursive Directory Crawling)
- KHÔNG BAO GIỜ duyệt đệ quy thư mục bộ nhớ ngoài (`/sdcard`) bằng `File.listFiles()` (thao tác này mất từ 10 - 30 giây do cơ chế Scoped Storage từ Android 10+).
- BẮT BUỘC ưu tiên truy vấn các loại file (APK, hình ảnh, video, âm thanh, tài liệu) thông qua `MediaStore` (`MediaStore.Files`, `MediaStore.Images`, v.v.). Hệ thống Android đã đánh chỉ mục sẵn trong SQLite, tốc độ trả về chỉ dưới 100ms.

### 3. Asynchronous Icon / Image Loading & Memory Cache
- KHÔNG giải mã (decode) Icon app (`pm.getApplicationIcon`) hoặc thumbnail file đồng bộ trong quá trình query dữ liệu trên ViewModel. Trả về metadata trước (`icon = null`) để giao diện hiển thị ngay lập tức không phải chờ đợi.
- Nạp Icon/ảnh bất đồng bộ (Lazy Loading) trên luồng nền (`Dispatchers.IO`) trong Adapter khi item hiển thị, và lưu vào bộ nhớ đệm (`LruCache` hoặc memory cache) để tránh nạp lại khi cuộn danh sách.

### 4. Quy chuẩn sử dụng Adapter có Checkbox / Selection (BaseApdaterSelected)
- Khi danh sách có tính năng chọn (checkbox, radio, highlight), BẮT BUỘC kế thừa `BaseApdaterSelected` thay vì `BaseAdapter` thông thường.
- Giữ nguyên cấu trúc quen thuộc: `setBinding`, `addListData`, `setData`, `onCLick`.
- Tách biệt rạch ròi 2 nhiệm vụ:
  + `setData`: Chỉ bind text, date, thumbnail, load icon bất đồng bộ (chỉ chạy khi item xuất hiện lần đầu). Tuyệt đối không xử lý checkbox ở hàm này.
  + `setSelection`: Chỉ cập nhật trạng thái chọn (ẩn/hiện checkbox, đổi drawable). Hàm này được gọi khi bind lần đầu và ĐƯỢC GỌI RIÊNG BIỆT qua Payload khi trạng thái chọn thay đổi.
- Quản lý trạng thái chọn qua ViewModel và `selectedKeys: Set<Any>`:
  + Fragment truyền `adapter.selectedKeys = selectedSet`. Adapter tự động diff O(1) và chỉ gọi `notifyItemChanged(index, PAYLOAD_SELECTION)` cho những item có trạng thái thay đổi.
  + KHÔNG BAO GIỜ gọi `notifyDataSetChanged()` khi chọn/bỏ chọn item.
  + Sự kiện click: Chỉ gọi `viewModel.toggle...` 1 lần duy nhất, không tự ý mutate state thủ công hoặc gọi lặp lại.

### 5. Quy chuẩn xử lý UI Sub-Tab / 2 Tab (Installed vs Not Installed, Categories)
- **Kiến trúc UDF (Single Source of Truth)**:
  + ViewModel quản lý enum sub-tab qua `_currentAppSubTab: MutableStateFlow<AppSubTab>`.
  + KHÔNG để Fragment tự lồng nhiều luồng `collect` giằng co dữ liệu giữa các tab (gây race condition, nhầm dữ liệu tab này sang tab kia).
  + BẮT BUỘC dùng `combine` trong ViewModel để tạo ra 2 stream duy nhất cho View:
    * `val currentDisplayApps: StateFlow<List<T>> = combine(subTab, dataTab1, dataTab2) { ... }`
    * `val isCurrentTabLoading: StateFlow<Boolean> = combine(subTab, loadingTab1, loadingTab2) { ... }`
  + Fragment chỉ cần `collect` duy nhất `currentDisplayApps` và `isCurrentTabLoading` để render UI và empty state.
- **Tối ưu Tải dữ liệu & Bộ nhớ đệm (Caching)**:
  + Khi người dùng chuyển sang tab mới, kiểm tra nếu `_dataTab.value.isNotEmpty()` thì KHÔNG tải lại từ đầu.
  + Dữ liệu tab cũ vẫn được giữ nguyên trong StateFlow, khi người dùng quay lại tab cũ, giao diện hiển thị ngay lập tức (0ms delay), không hiển thị progress loading hay giật màn hình.
- **Đồng bộ Selection xuyên suốt các tab**:
  + Danh sách file/item được chọn (`selectedFiles`) phải được quản lý tập trung ở cấp ViewModel, dùng chung cho tất cả các tab.
  + Khi người dùng chuyển tab qua lại, trạng thái checkbox của các file đã chọn ở tab trước vẫn được bảo lưu chính xác.

