# Plan Phát Triển Tính Năng Send & Receive (ShareIt Clone)

> **Tài liệu này mô tả chi tiết từng bước phát triển tính năng Send & Receive.**
> Dựa trên phân tích ảnh thiết kế (Figma/mockup) và so sánh với app gốc ShareIt.

---

## MỤC LỤC

1. [Phân tích thiết kế hiện có](#1-phân-tích-thiết-kế-hiện-có)
2. [So sánh với ShareIt gốc — Phần còn thiếu](#2-so-sánh-với-shareit-gốc--phần-còn-thiếu)
3. [Đề xuất bổ sung thiết kế](#3-đề-xuất-bổ-sung-thiết-kế)
4. [Danh sách đầy đủ màn hình cần implement](#4-danh-sách-đầy-đủ-màn-hình-cần-implement)
5. [Luồng chuyển màn hình (Flow)](#5-luồng-chuyển-màn-hình-flow)
6. [Công nghệ / Kỹ thuật implement](#6-công-nghệ--kỹ-thuật-implement)
7. [Kiến trúc Android](#7-kiến-trúc-android)
8. [Plan phát triển từng bước](#8-plan-phát-triển-từng-bước-chi-tiết)
9. [Open Questions](#9-open-questions)

---

## 1. Phân Tích Thiết Kế Hiện Có

### 1.1 Danh Sách Màn Hình Trong Mockup

| # | Màn hình | Mô tả |
|---|----------|-------|
| S1 | **Send Files - Tab Recent** | Danh sách file gần đây, có checkbox chọn, nút Send ở bottom |
| S2 | **Send Files - Tab Installed** | Danh sách app đã cài (list view), checkbox chọn |
| S3 | **Send Files - Tab File** | Danh sách file tài liệu/documents |
| S4 | **Send Files - Tab Photo** | Grid ảnh thumbnail, checkbox overlay, hỗ trợ chọn nhiều |
| S5 | **Send Files - Tab Video** | Danh sách video với thumbnail, duration |
| S6 | **Send Files - Tab Music** | Danh sách nhạc, hiển thị tên bài, nghệ sĩ |
| S7 | **Send Files - Apps Grid** | Hiển thị apps dạng grid icon |
| S8 | **QR Scanner (Send side)** | Camera preview + khung scan QR code, text "Scan and Send" |
| S9 | **Receive Screen** | QR code lớn + "Ready to Receive" + Device Name + WiFi Network + IP Address |
| S10 | **Send/Receive Match** | Hiển thị tốc độ (2.3 MB/s), số thiết bị (04), QR code pairing, nút Connect |

### 1.2 Thành Phần UI Chi Tiết

**Màn hình Send Files (S1-S7):**
- Toolbar: nút Back, title "Send Files"
- TabLayout: 6 tabs — Recent | Installed | File | Photo | Video | Music
- RecyclerView: list/grid tùy tab
- Item UI: icon/thumbnail + tên file + size + checkbox
- Bottom bar: counter "X files selected" + nút Send (màu xanh primary)

**Màn hình QR Scanner (S8):**
- Camera preview full screen
- Khung scan QR hình vuông ở giữa
- Text hướng dẫn "Scan code to send files"

**Màn hình Receive (S9):**
- QR Code lớn ở trung tâm (~200dp)
- Text "Ready to Receive"
- Text "Other devices can find you in the same WiFi"
- Card info: Device Name, WiFi Network, IP Address

**Màn hình Match/Connection (S10):**
- Header: tốc độ truyền + số thiết bị
- QR code nhỏ hơn
- Nút "Connect"
- Status dialog (có trường hợp lỗi)

### 1.3 Component Đã Có Trong Project

- `layout_local_hotspot.xml` — Card hiển thị Hotspot info (SSID, Password, IP, QR, switch on/off)
- `layout_quick_tools.xml` — Quick Tools có sẵn icon: Clone Phone, Group Share, Nearby Play
- `layout_bottom_navigation.xml` — Bottom nav: Home | FAB(+) | Scan
- `activity_main.xml` — Đã include `layout_local_hotspot`

---

## 2. So Sánh Với ShareIt Gốc — Phần Còn Thiếu

### 2.1 Màn Hình Thiếu

| # | Màn hình thiếu | Mức độ | Mô tả |
|---|---------------|--------|-------|
| ❌1 | **Device Discovery / Radar** | 🔴 Critical | Tìm kiếm thiết bị xung quanh, radar animation quét vòng tròn |
| ❌2 | **Transfer Progress** | 🔴 Critical | Tiến trình từng file: progress bar, tốc độ, thời gian còn lại |
| ❌3 | **Transfer Complete / Summary** | 🟡 Important | Tổng kết: số file, dung lượng, thời gian, nút "View Files" |
| ❌4 | **Transfer History** | 🟡 Important | Lịch sử gửi/nhận, phân loại theo ngày |
| ❌5 | **Received Files Manager** | 🟡 Important | Quản lý file đã nhận, phân loại theo loại |
| ❌6 | **Connection Failed / Retry** | 🟡 Important | Xử lý lỗi kết nối, nút Retry |
| ❌7 | **Accept/Reject Dialog (Receiver)** | 🟡 Important | Xác nhận nhận file trên máy receiver |
| ❌8 | **Sending Waiting Screen** | 🟢 Nice to have | Sender chờ receiver chấp nhận |
| ❌9 | **Permission Setup** | 🟢 Nice to have | Hướng dẫn bật WiFi, Location cho transfer |

### 2.2 Thành Phần UI Thiếu

| # | Component | Mô tả |
|---|-----------|-------|
| ❌1 | Notification khi đang truyền | Foreground notification với progress bar |
| ❌2 | Mini transfer indicator | Floating indicator khi user rời màn hình transfer |
| ❌3 | File preview trước khi nhận | Thumbnail/icon cho từng file được gửi |
| ❌4 | Speed indicator realtime | Tốc độ truyền cập nhật liên tục |
| ❌5 | Device avatar/profile | Ảnh đại diện thiết bị |

### 2.3 Luồng Thiếu

| # | Flow | Mô tả |
|---|------|-------|
| ❌1 | Auto-reconnect | Tự kết nối lại khi bị gián đoạn |
| ❌2 | Resume transfer | Tiếp tục truyền file bị gián đoạn |
| ❌3 | Background transfer | Tiếp tục truyền khi app ở background |
| ❌4 | Cancel transfer | Hủy quá trình truyền giữa chừng |
| ❌5 | Multi-device send | Gửi cùng lúc cho nhiều thiết bị |

---

## 3. Đề Xuất Bổ Sung Thiết Kế

### M1: Device Discovery Screen (🔴 Critical)
```
┌─────────────────────────┐
│  ← Searching...         │
│                         │
│     ┌─────────┐         │
│    /  Radar    \        │
│   │  Animation  │       │
│   │   🔵 Phone  │       │
│    \           /        │
│     └─────────┘         │
│                         │
│  Found 2 devices:       │
│  ┌──────────────────┐   │
│  │ 📱 Samsung A54   │   │
│  └──────────────────┘   │
│  ┌──────────────────┐   │
│  │ 📱 iPhone 15     │   │
│  └──────────────────┘   │
│                         │
│  [  Scan QR Instead  ]  │
└─────────────────────────┘
```

### M2: Transfer Progress Screen (🔴 Critical)
```
┌─────────────────────────┐
│  ← Transferring         │
│  To: Samsung A54  📱    │
│  Speed: 23.5 MB/s       │
│                         │
│  ━━━━━━━━━━━━░░░ 67%    │
│  12/18 files • 3.2 GB   │
│                         │
│  ┌──────────────────┐   │
│  │ ✅ photo1.jpg 2MB│   │
│  │ ✅ video.mp4 50MB│   │
│  │ ⏳ doc.pdf  5MB  │   │
│  │ ⬜ app.apk  20MB │   │
│  └──────────────────┘   │
│                         │
│  [     Cancel     ]     │
└─────────────────────────┘
```

### M3: Transfer Complete Screen
```
┌─────────────────────────┐
│        ✅               │
│  Transfer Complete!     │
│                         │
│  18 files sent          │
│  Total: 4.8 GB          │
│  Time: 3m 24s           │
│  Avg Speed: 23.5 MB/s   │
│                         │
│  [  View Files  ]       │
│  [  Send More   ]       │
│  [    Done      ]       │
└─────────────────────────┘
```

### M4: Accept/Reject Dialog (Receiver side)
```
┌─────────────────────────┐
│  Incoming Transfer      │
│  From: Samsung A54      │
│                         │
│  5 files (230 MB)       │
│  ┌──────────────────┐   │
│  │ 📷 photo1.jpg    │   │
│  │ 📷 photo2.jpg    │   │
│  │ 🎵 song.mp3      │   │
│  └──────────────────┘   │
│                         │
│  [Reject]  [Accept]     │
└─────────────────────────┘
```

---

## 4. Danh Sách Đầy Đủ Màn Hình Cần Implement

| # | Màn hình | Class | Trạng thái |
|---|----------|-------|-----------|
| 1 | Send Files (chọn file) | `SendFilesActivity` + 6 Fragments | ✅ Có thiết kế |
| 2 | QR Scanner (bên Send) | `QrScannerActivity` | ✅ Có thiết kế |
| 3 | Receive (hiển thị QR) | `ReceiveActivity` | ✅ Có thiết kế |
| 4 | Send/Receive Match | `ConnectionActivity` | ✅ Có thiết kế |
| 5 | Device Discovery | `DeviceDiscoveryActivity` | ❌ Cần bổ sung |
| 6 | Transfer Progress | `TransferProgressActivity` | ❌ Cần bổ sung |
| 7 | Transfer Complete | `TransferCompleteActivity` | ❌ Cần bổ sung |
| 8 | Transfer History | `TransferHistoryActivity` | ❌ Cần bổ sung |
| 9 | Received Files | `ReceivedFilesActivity` | ❌ Cần bổ sung |
| 10 | Accept/Reject Dialog | `IncomingTransferDialog` | ❌ Cần bổ sung |
| 11 | Connection Error | `ConnectionErrorDialog` | ❌ Cần bổ sung |

---

## 5. Luồng Chuyển Màn Hình (Flow)

### 5.1 Send Flow
```
Home → [Tap Send] → SendFilesActivity
  → Chọn files qua 6 tabs (Recent/Installed/File/Photo/Video/Music)
  → [Tap Send button]
  → Chọn phương thức:
    → Option A: QrScannerActivity → Scan QR từ receiver
    → Option B: DeviceDiscoveryActivity → Chọn thiết bị tìm thấy
  → ConnectionActivity (đang kết nối...)
    → Thành công → TransferProgressActivity → TransferCompleteActivity
    → Thất bại → ConnectionErrorDialog → Retry / Cancel
```

### 5.2 Receive Flow
```
Home → [Tap Receive] → ReceiveActivity
  → Tạo Hotspot → Generate QR → Hiển thị QR + device info
  → Chờ sender kết nối...
  → Sender kết nối → IncomingTransferDialog (Accept/Reject)
    → Accept → TransferProgressActivity (nhận file) → TransferCompleteActivity
    → Reject → Quay lại ReceiveActivity
```

### 5.3 History Flow
```
Home → [Tap History] → TransferHistoryActivity
  → Tab: Sent | Received | All
  → Tap item → ReceivedFilesActivity (xem chi tiết file)
```

### 5.4 Flow Diagram Đầy Đủ
```
                    ┌──────────┐
                    │   HOME   │
                    └────┬─────┘
              ┌──────────┼──────────┐
              ▼          ▼          ▼
         ┌────────┐ ┌────────┐ ┌─────────┐
         │  SEND  │ │RECEIVE │ │ HISTORY │
         └───┬────┘ └───┬────┘ └────┬────┘
             ▼          ▼           ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │Select    │ │Show QR + │ │List:     │
      │Files     │ │Wait conn │ │Sent/Recv │
      │(6 tabs)  │ │          │ │/All      │
      └───┬──────┘ └───┬──────┘ └──────────┘
          ▼            ▼
    ┌─────────┐  ┌──────────┐
    │QR Scan  │  │Incoming  │
    │  OR     │  │Transfer  │
    │Discovery│  │Dialog    │
    └───┬─────┘  └─┬────┬──┘
        ▼          ▼    ▼
   ┌──────────┐ Accept Reject
   │Connection│   ▼      ▼
   │Activity  │   │   Back to
   └─┬────┬──┘   │   Receive
     ▼    ▼      ▼
   OK   Error ┌──────────┐
   ▼    ▼     │Transfer  │
   │  Retry   │Progress  │
   │    │     └────┬─────┘
   │    │          ▼
   │    │     ┌──────────┐
   └────┴────►│Transfer  │
              │Complete  │
              └────┬─────┘
                   ▼
            ┌──────────────┐
            │View Files /  │
            │Send More /   │
            │Done → Home   │
            └──────────────┘
```

---

## 6. Công Nghệ / Kỹ Thuật Implement

### 6.1 Cơ Chế Truyền File P2P (Giống ShareIt)

**ShareIt dùng: Wi-Fi Hotspot + TCP Socket** — KHÔNG dùng Bluetooth hay Wi-Fi Direct API thuần.

#### Phương án: Wi-Fi Hotspot + TCP Socket

**Receiver (Server):**
1. Tạo Wi-Fi Hotspot (`LocalOnlyHotspot` API)
2. Generate QR chứa: `{ssid, password, ip, port}`
3. Khởi tạo `ServerSocket` lắng nghe port
4. Chờ client kết nối

**Sender (Client):**
1. Scan QR → parse `{ssid, password, ip, port}`
2. Kết nối Wi-Fi Hotspot của receiver (`WifiNetworkSpecifier`)
3. Tạo `Socket` kết nối đến `ServerSocket`
4. Gửi metadata → gửi file qua `OutputStream`

#### Giao thức truyền:
```
[SENDER]                              [RECEIVER]
   |--- TCP Connect ------------------->  |
   |--- HANDSHAKE {deviceName, count} ->  |
   |<-- ACK_HANDSHAKE -------------------  |
   |--- FILE_META {name, size, type} --->  |
   |<-- ACK_META / REJECT ---------------  |
   |--- FILE_DATA (chunked 8KB) -------->  |
   |<-- ACK_FILE {checksum} -------------  |
   |--- (repeat for next file) --------->  |
   |--- TRANSFER_COMPLETE -------------->  |
   |<-- ACK_COMPLETE --------------------  |
```

### 6.2 Bảng Kỹ Thuật

| Kỹ thuật | Vai trò | Lý do |
|----------|---------|-------|
| `WifiManager.LocalOnlyHotspotCallback` | Tạo hotspot (receiver) | API chính thức, API 26+, không cần root |
| `WifiNetworkSpecifier` | Sender kết nối WiFi | API 29+ (khớp minSdk=29) |
| `ServerSocket` / `Socket` (TCP) | Kênh truyền dữ liệu | Tốc độ cao, reliable |
| `ZXing` + `ML Kit Barcode` | Generate/Scan QR | Mature, camera API tốt |
| `NsdManager` (mDNS) | Device discovery | Tìm thiết bị cùng mạng |
| JSON | Metadata exchange | Handshake, file info |
| `CameraX` | Camera cho QR scanner | API modern, lifecycle-aware |

### 6.3 Fallback Strategy
```
Ưu tiên 1: LocalOnlyHotspot + TCP Socket      (~20-50 MB/s)
Ưu tiên 2: Wi-Fi Direct (WifiP2pManager)      (~15-30 MB/s)
Ưu tiên 3: Same WiFi Network + TCP Socket     (~10-20 MB/s)
```

---

## 7. Kiến Trúc Android

### 7.1 Package Structure
```
com.example.basekotlin/
├── ui/transfer/                           # Package mới
│   ├── send/
│   │   ├── SendFilesActivity.kt
│   │   ├── SendFilesViewModel.kt
│   │   ├── adapter/
│   │   │   ├── FileSelectionAdapter.kt
│   │   │   ├── AppSelectionAdapter.kt
│   │   │   └── PhotoSelectionAdapter.kt
│   │   └── fragment/
│   │       ├── RecentFragment.kt
│   │       ├── InstalledAppsFragment.kt
│   │       ├── FileFragment.kt
│   │       ├── PhotoFragment.kt
│   │       ├── VideoFragment.kt
│   │       └── MusicFragment.kt
│   ├── receive/
│   │   ├── ReceiveActivity.kt
│   │   └── ReceiveViewModel.kt
│   ├── scanner/
│   │   └── QrScannerActivity.kt
│   ├── discovery/
│   │   ├── DeviceDiscoveryActivity.kt
│   │   └── DeviceDiscoveryViewModel.kt
│   ├── progress/
│   │   ├── TransferProgressActivity.kt
│   │   └── TransferProgressViewModel.kt
│   ├── complete/
│   │   └── TransferCompleteActivity.kt
│   ├── history/
│   │   ├── TransferHistoryActivity.kt
│   │   ├── TransferHistoryViewModel.kt
│   │   └── adapter/HistoryAdapter.kt
│   ├── received/
│   │   ├── ReceivedFilesActivity.kt
│   │   └── ReceivedFilesViewModel.kt
│   ├── dialog/
│   │   ├── IncomingTransferDialog.kt
│   │   └── ConnectionErrorDialog.kt
│   └── model/
│       ├── TransferFile.kt
│       ├── TransferSession.kt
│       └── DeviceInfo.kt
│
├── data/local/transfer/                   # Room DB
│   ├── TransferDatabase.kt
│   ├── dao/TransferHistoryDao.kt
│   └── entity/TransferHistoryEntity.kt
│
├── service/transfer/                      # Background services
│   ├── TransferService.kt                # Foreground Service chính
│   ├── FileServerService.kt             # TCP Server (receiver)
│   ├── FileSenderService.kt             # TCP Client (sender)
│   └── HotspotManager.kt                # Quản lý Hotspot
│
└── util/transfer/                         # Utilities
    ├── QrCodeHelper.kt
    ├── WifiHelper.kt
    ├── FileTransferProtocol.kt
    ├── TransferNotificationHelper.kt
    └── NetworkUtils.kt
```

### 7.2 Permissions Cần Bổ Sung
```xml
<!-- WiFi & Hotspot -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

<!-- Location (bắt buộc cho WiFi scan trên Android 10+) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Nearby devices (Android 12+) -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation" />

<!-- Camera cho QR Scanner -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Foreground Service cho transfer -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
```

### 7.3 Dependencies Cần Thêm
```groovy
// QR Code generate & scan
implementation 'com.google.zxing:core:3.5.3'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// CameraX (cho QR scanner)
implementation 'androidx.camera:camera-camera2:1.5.0'
implementation 'androidx.camera:camera-lifecycle:1.5.0'
implementation 'androidx.camera:camera-view:1.5.0'
implementation 'com.google.mlkit:barcode-scanning:17.3.0'

// Lottie animation (radar, loading effects)
implementation 'com.airbnb.android:lottie:6.4.0'
```

### 7.4 Data Models Chính
```kotlin
// Thông tin kết nối encode trong QR
data class ConnectionInfo(
    val ssid: String,
    val password: String,
    val ipAddress: String,
    val port: Int,
    val deviceName: String
)

// File cần truyền
data class TransferFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val status: TransferStatus = TransferStatus.PENDING
)

enum class TransferStatus {
    PENDING, TRANSFERRING, COMPLETED, FAILED
}

// Trạng thái transfer session
sealed class TransferState {
    object Idle : TransferState()
    object Connecting : TransferState()
    data class Connected(val device: DeviceInfo) : TransferState()
    data class Transferring(
        val currentFile: TransferFile,
        val currentIndex: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val speed: Float // MB/s
    ) : TransferState()
    data class Complete(val summary: TransferSummary) : TransferState()
    data class Error(val message: String, val canRetry: Boolean) : TransferState()
}

// Room Entity cho lịch sử
@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val direction: String, // "SENT" or "RECEIVED"
    val fileCount: Int,
    val totalSize: Long,
    val timestamp: Long,
    val duration: Long,
    val avgSpeed: Float,
    val fileList: String // JSON serialized
)
```

---

## 8. Plan Phát Triển Từng Bước Chi Tiết

---

### PHASE 1: Core Infrastructure (Tuần 1-2)
> Mục tiêu: Xây dựng nền tảng kết nối và truyền file giữa 2 thiết bị

#### Bước 1.1: Setup Project
- [ ] Thêm permissions vào `AndroidManifest.xml`
- [ ] Thêm dependencies vào `build.gradle`
- [ ] Tạo package structure: `ui/transfer/`, `service/transfer/`, `util/transfer/`, `data/local/transfer/`
- [ ] Tạo data models: `TransferFile.kt`, `ConnectionInfo.kt`, `DeviceInfo.kt`, `TransferState.kt`
- [ ] Tạo string resources cho tất cả text (values/strings.xml)

#### Bước 1.2: HotspotManager
- [ ] `HotspotManager.kt` — Tạo/quản lý `LocalOnlyHotspot`
  - `startHotspot(callback: (HotspotInfo) -> Unit)` — bật hotspot, trả về SSID + password
  - `stopHotspot()` — tắt hotspot
  - `getConnectionInfo(): ConnectionInfo` — lấy SSID, password, IP, port
- [ ] Test: bật hotspot → log SSID, password → tắt hotspot

#### Bước 1.3: QrCodeHelper
- [ ] `QrCodeHelper.kt`
  - `generateQrBitmap(info: ConnectionInfo, size: Int): Bitmap` — tạo QR từ JSON
  - `parseQrContent(content: String): ConnectionInfo?` — parse JSON từ QR scan
  - Format QR: `{"ssid":"xxx","password":"xxx","ip":"192.168.x.x","port":8888,"device":"Phone Name"}`
- [ ] Test: generate QR → scan lại → verify data đúng

#### Bước 1.4: WifiHelper
- [ ] `WifiHelper.kt` — Sender kết nối WiFi của receiver
  - `connectToWifi(ssid: String, password: String, callback: (Boolean) -> Unit)`
  - Sử dụng `WifiNetworkSpecifier` + `NetworkRequest` (API 29+)
  - `disconnectWifi()` — ngắt kết nối
- [ ] Test: kết nối WiFi programmatically → verify connected

#### Bước 1.5: TCP Socket — FileServerService (Receiver)
- [ ] `FileServerService.kt`
  - `startServer(port: Int)` — khởi tạo `ServerSocket`
  - `acceptConnection(): Socket` — chờ client kết nối
  - `receiveHandshake(socket: Socket): HandshakeData` — nhận device info + file count
  - `receiveFile(socket: Socket, savePath: File): TransferFile` — nhận 1 file
    - Đọc metadata (JSON header: name, size, type)
    - Đọc file data (chunked 8KB buffer)
    - Verify bằng size comparison
  - `sendAck(socket: Socket, status: String)` — gửi acknowledgment
- [ ] Test: chạy server trên emulator → netcat kết nối → gửi test data

#### Bước 1.6: TCP Socket — FileSenderService (Sender)
- [ ] `FileSenderService.kt`
  - `connect(ip: String, port: Int): Socket` — kết nối đến server
  - `sendHandshake(socket: Socket, deviceName: String, fileCount: Int)` — gửi thông tin
  - `sendFile(socket: Socket, file: TransferFile, onProgress: (Long, Long) -> Unit)`
    - Gửi metadata header (JSON)
    - Gửi file data (chunked, BufferedOutputStream)
    - Đọc ACK response
  - `sendAllFiles(socket: Socket, files: List<TransferFile>, onProgress: ...)` — gửi toàn bộ
- [ ] Test: 2 emulator → sender gửi file → receiver nhận → verify file

#### Bước 1.7: TransferService (Foreground Service)
- [ ] `TransferService.kt` — Foreground Service quản lý lifecycle
  - Bind với Activity qua `Binder`
  - Expose `StateFlow<TransferState>` để UI observe
  - Expose `StateFlow<FileProgress>` cho progress realtime
  - Hiển thị notification với progress bar
  - Handle lifecycle: app background → tiếp tục truyền
- [ ] `TransferNotificationHelper.kt` — tạo/update notification
  - Channel: "File Transfer"
  - Content: tên file đang truyền + progress %
  - Actions: Cancel
- [ ] Test: start service → transfer file → verify notification + progress

#### Bước 1.8: NetworkUtils
- [ ] `NetworkUtils.kt`
  - `getLocalIpAddress(): String` — lấy IP local
  - `isWifiConnected(): Boolean` — check WiFi status
  - `getAvailablePort(): Int` — tìm port trống
  - `formatSpeed(bytesPerSec: Long): String` — format "23.5 MB/s"
  - `formatFileSize(bytes: Long): String` — format "2.3 GB"

#### ✅ Milestone Phase 1: 2 thiết bị có thể truyền file qua Hotspot + Socket thành công

---

### PHASE 2: UI — Send Flow (Tuần 2-3)
> Mục tiêu: Hoàn thiện giao diện chọn file và gửi

#### Bước 2.1: SendFilesActivity — Layout & TabLayout
- [ ] Tạo `activity_send_files.xml`
  - Toolbar (back + title "Send Files")
  - TabLayout: 6 tabs (Recent | Installed | File | Photo | Video | Music)
  - ViewPager2 cho fragments
  - Bottom bar: `tvSelectedCount` + `btnSend` (ẩn khi chưa chọn, slide up animation)
- [ ] Tạo `SendFilesActivity.kt`
  - Setup ViewPager2 + TabLayoutMediator
  - SharedViewModel để giữ trạng thái chọn file xuyên tabs
  - Observe selected files → update counter + show/hide bottom bar
- [ ] Tạo `SendFilesViewModel.kt`
  - `selectedFiles: MutableStateFlow<List<TransferFile>>` — danh sách file đã chọn
  - `addFile(file: TransferFile)` / `removeFile(file: TransferFile)`
  - `clearSelection()`
  - `getSelectedCount(): Int`

#### Bước 2.2: RecentFragment
- [ ] Tạo `fragment_send_recent.xml` — RecyclerView
- [ ] Tạo `item_send_file.xml` — thumbnail + name + size + date + checkbox
- [ ] Tạo `RecentFragment.kt`
  - Query `MediaStore` cho file mới nhất (tất cả loại)
  - Sắp xếp theo `DATE_MODIFIED` DESC
  - Click item → toggle selection trong SharedViewModel
- [ ] Tạo `FileSelectionAdapter.kt` — RecyclerView.Adapter với DiffUtil

#### Bước 2.3: AppsFragment
- [ ] Tạo `fragment_send_apps.xml` — RecyclerView (grid 3 cột hoặc list tùy toggle)
- [ ] Tạo `item_send_app.xml` — app icon + name + size + checkbox
- [ ] Tạo `InstalledAppsFragment.kt`
  - `PackageManager.getInstalledApplications()` → filter user apps
  - Extract APK path → tạo `TransferFile`
  - Toggle grid/list view
- [ ] Tạo `AppSelectionAdapter.kt`

#### Bước 2.4: FileFragment (Documents)
- [ ] Tạo `fragment_send_file.xml`
- [ ] Tạo `FileFragment.kt`
  - Query `MediaStore.Files` cho documents (pdf, doc, xls, ppt, txt...)
  - Filter theo MIME type
  - Hiển thị icon theo loại file

#### Bước 2.5: PhotoFragment
- [ ] Tạo `fragment_send_photo.xml` — RecyclerView grid (3-4 cột)
- [ ] Tạo `item_send_photo.xml` — thumbnail full + checkbox overlay góc phải
- [ ] Tạo `PhotoFragment.kt`
  - Query `MediaStore.Images`
  - Load thumbnail bằng Glide
  - Multi-select với counter badge
- [ ] Tạo `PhotoSelectionAdapter.kt` — grid adapter

#### Bước 2.6: VideoFragment
- [ ] Tạo `fragment_send_video.xml`
- [ ] Tạo `item_send_video.xml` — thumbnail + title + duration + size + checkbox
- [ ] Tạo `VideoFragment.kt`
  - Query `MediaStore.Video`
  - Format duration: "3:24"

#### Bước 2.7: MusicFragment
- [ ] Tạo `fragment_send_music.xml`
- [ ] Tạo `item_send_music.xml` — album art + title + artist + duration + checkbox
- [ ] Tạo `MusicFragment.kt`
  - Query `MediaStore.Audio`
  - Filter non-notification sounds

#### Bước 2.8: QrScannerActivity
- [ ] Tạo `activity_qr_scanner.xml`
  - CameraX PreviewView (full screen)
  - Overlay: khung scan vuông (viền xanh, ngoài khung tối mờ)
  - Text hướng dẫn: "Scan QR code from receiver"
  - Nút flash toggle
  - Nút back
- [ ] Tạo `QrScannerActivity.kt`
  - Setup CameraX + ML Kit BarcodeScanner
  - Analyze frame → detect QR → parse `ConnectionInfo`
  - Vibrate + sound khi scan thành công
  - Auto-navigate sang kết nối
- [ ] Handle permission: Camera permission request

#### Bước 2.9: Kết Nối Send Flow
- [ ] `SendFilesActivity` → nhấn Send → chuyển sang `QrScannerActivity`
  - Truyền `ArrayList<TransferFile>` qua Intent (Parcelable)
- [ ] `QrScannerActivity` → scan thành công → kết nối WiFi → start `TransferService`
  - Hiển thị loading "Connecting..."
  - Connect WiFi → Connect Socket → Start transfer
  - Chuyển sang `TransferProgressActivity`

#### ✅ Milestone Phase 2: User có thể chọn file → scan QR → bắt đầu gửi

---

### PHASE 3: UI — Receive Flow (Tuần 3-4)
> Mục tiêu: Hoàn thiện giao diện nhận file

#### Bước 3.1: ReceiveActivity — Layout
- [ ] Tạo `activity_receive.xml`
  - Toolbar (back + title "Receive")
  - ImageView QR code lớn (~200dp, bo góc, shadow)
  - Text "Scan code to send files" (hint nhỏ)
  - Text "Ready to Receive" (bold, 20sp)
  - Text "Other devices can find you in the same WiFi"
  - Card info:
    - Row 1: icon 📱 + "Device's Name" + device name (bold)
    - Row 2: icon 📶 + "WiFi Network" + network name
    - Row 3: icon 🌐 + "IP Address" + IP (màu xanh)
  - Loading animation dưới cùng
- [ ] Tạo `ReceiveActivity.kt`
  - `onCreate`: Check permissions → Start Hotspot → Generate QR → Start Server
  - `ReceiveViewModel`: manage hotspot state, server state
  - Observe: HotspotState → update UI (QR, SSID, IP...)
  - Observe: ServerState → khi client connect → show IncomingTransferDialog

#### Bước 3.2: ReceiveViewModel
- [ ] `ReceiveViewModel.kt`
  - `hotspotState: StateFlow<HotspotState>` — STARTING / ACTIVE / ERROR
  - `serverState: StateFlow<ServerState>` — WAITING / CLIENT_CONNECTED / RECEIVING
  - `connectionInfo: StateFlow<ConnectionInfo?>` — thông tin QR
  - `startReceiving()` — bật hotspot + start server
  - `stopReceiving()` — tắt hotspot + stop server
  - `acceptTransfer()` / `rejectTransfer()`

#### Bước 3.3: IncomingTransferDialog
- [ ] Tạo `dialog_incoming_transfer.xml`
  - Title: "Incoming Transfer"
  - Subtitle: "From: {deviceName}"
  - Text: "{fileCount} files ({totalSize})"
  - RecyclerView: danh sách file (icon + name + size)
  - 2 nút: Reject (outline) + Accept (filled green)
- [ ] Tạo `IncomingTransferDialog.kt`
  - Nhận `HandshakeData` từ ReceiveActivity
  - Callback: `onAccept()` / `onReject()`
  - Auto timeout: 30s → auto reject

#### Bước 3.4: ConnectionActivity
- [ ] Tạo `activity_connection.xml`
  - Header: speed display + device count
  - QR code nhỏ
  - Connection status animation (pulse effect)
  - Device info
  - Nút Connect / Cancel
- [ ] Tạo `ConnectionActivity.kt`
  - States: Scanning → Connecting → Connected → Error
  - Observe `TransferService.transferState`

#### Bước 3.5: TransferProgressActivity
- [ ] Tạo `activity_transfer_progress.xml`
  - Header: device name + speed indicator
  - ProgressBar tổng (horizontal, custom style xanh)
  - Text: "{current}/{total} files • {transferredSize}"
  - RecyclerView: danh sách file
    - item_transfer_file.xml: icon status (✅⏳⬜) + name + size + mini progress
  - Nút Cancel (bottom)
- [ ] Tạo `TransferProgressActivity.kt`
  - Bind `TransferService`
  - Observe `transferState` → update UI realtime
  - Observe `fileProgress` → update individual file progress
  - Handle Cancel → confirm dialog → stop service
- [ ] Tạo `TransferProgressViewModel.kt`
  - Calculate speed, ETA, overall progress
- [ ] Tạo `TransferFileAdapter.kt` — adapter cho danh sách file trong progress

#### Bước 3.6: TransferCompleteActivity
- [ ] Tạo `activity_transfer_complete.xml`
  - Success icon + animation (Lottie checkmark)
  - Title: "Transfer Complete!"
  - Stats: files count, total size, duration, avg speed
  - 3 nút: View Files / Send More / Done
- [ ] Tạo `TransferCompleteActivity.kt`
  - Nhận `TransferSummary` từ Intent
  - "View Files" → `ReceivedFilesActivity`
  - "Send More" → `SendFilesActivity`
  - "Done" → finish → Home
  - Lưu vào Transfer History (Room DB)

#### Bước 3.7: ConnectionErrorDialog
- [ ] Tạo `dialog_connection_error.xml`
  - Error icon
  - Title: "Connection Failed"
  - Message: mô tả lỗi cụ thể
  - Troubleshooting tips (bullet points)
  - 2 nút: Cancel + Retry
- [ ] Tạo `ConnectionErrorDialog.kt`

#### ✅ Milestone Phase 3: Flow Send + Receive hoàn chỉnh, 2 thiết bị truyền file thành công end-to-end

---

### PHASE 4: Supplementary Features (Tuần 4-5)
> Mục tiêu: Hoàn thiện các tính năng phụ

#### Bước 4.1: Device Discovery (NSD/mDNS)
- [ ] Tạo `activity_device_discovery.xml`
  - Radar animation (Lottie hoặc custom Canvas)
  - RecyclerView danh sách thiết bị tìm thấy
  - item_discovered_device.xml: icon + name + IP
  - Nút "Scan QR Instead" (bottom)
  - Pull to refresh
- [ ] Tạo `DeviceDiscoveryActivity.kt`
  - Sử dụng `NsdManager` để discover services
  - Service type: `_shafile._tcp.`
  - Animate thiết bị xuất hiện trên radar
  - Click thiết bị → kết nối trực tiếp (không cần QR)
- [ ] Tạo `DeviceDiscoveryViewModel.kt`
  - `discoveredDevices: StateFlow<List<DeviceInfo>>`
  - `startDiscovery()` / `stopDiscovery()`
- [ ] Cập nhật `ReceiveActivity`: register NSD service khi bật receiver

#### Bước 4.2: Transfer History — Room DB
- [ ] Tạo `TransferHistoryEntity.kt` — Room Entity
- [ ] Tạo `TransferHistoryDao.kt`
  - `@Insert` insertHistory
  - `@Query` getAllHistory, getSentHistory, getReceivedHistory
  - `@Query` getHistoryByDate
  - `@Delete` deleteHistory
- [ ] Tạo/Update `TransferDatabase.kt` — thêm entity vào Room DB
- [ ] Tạo `TransferHistoryRepository.kt`

#### Bước 4.3: Transfer History — UI
- [ ] Tạo `activity_transfer_history.xml`
  - TabLayout: Sent | Received | All
  - ViewPager2 + Fragments
  - RecyclerView group theo ngày
  - Empty state khi chưa có lịch sử
- [ ] Tạo `item_transfer_history.xml`
  - Direction icon (↑ sent / ↓ received)
  - Device name
  - File count + total size
  - Timestamp
- [ ] Tạo `TransferHistoryActivity.kt` + `TransferHistoryViewModel.kt`
- [ ] Tạo `HistoryAdapter.kt` với section header (Today, Yesterday, date...)

#### Bước 4.4: Received Files Manager
- [ ] Tạo `activity_received_files.xml`
  - TabLayout: All | Photo | Video | Music | App | Doc
  - RecyclerView
  - Selection mode: long press → select → actions (Open/Share/Delete)
- [ ] Tạo `ReceivedFilesActivity.kt`
  - Scan thư mục received files
  - Phân loại theo MIME type
  - Open file bằng Intent
- [ ] Tạo `ReceivedFilesViewModel.kt`

#### Bước 4.5: Tích Hợp Vào Home Screen
- [ ] Cập nhật `MainActivity.kt`
  - `btnSend` / layout entry → navigate sang `SendFilesActivity`
  - `btnReceive` / layout entry → navigate sang `ReceiveActivity`
  - `btnHistory` → navigate sang `TransferHistoryActivity`
- [ ] Cập nhật Quick Tools nếu cần (thêm Send/Receive shortcut)
- [ ] Cập nhật Bottom Navigation nếu cần

#### ✅ Milestone Phase 4: Tính năng hoàn chỉnh với history, device discovery, file manager

---

### PHASE 5: Polish & Optimization (Tuần 5-6)
> Mục tiêu: Hoàn thiện UX, animation, edge cases

#### Bước 5.1: Animations
- [ ] Radar animation cho Device Discovery (Lottie hoặc custom)
- [ ] QR scan frame animation (viền chạy)
- [ ] Transfer progress: smooth progress bar animation
- [ ] Transfer complete: Lottie checkmark celebration
- [ ] Screen transitions: shared element transition
- [ ] Bottom bar slide up/down khi chọn/bỏ chọn file

#### Bước 5.2: Edge Cases & Error Handling
- [ ] Interrupted transfer → resume hoặc restart
- [ ] Large file support (>1GB) → chunked transfer với progress
- [ ] WiFi disconnected giữa chừng → auto retry 3 lần → show error
- [ ] App killed → TransferService tiếp tục → notification
- [ ] Low storage → warning trước khi nhận
- [ ] Battery optimization → whitelist request
- [ ] Permission denied → hướng dẫn bật trong Settings

#### Bước 5.3: Performance Optimization
- [ ] Buffer size tuning (8KB → 64KB → benchmark)
- [ ] File I/O: BufferedInputStream/OutputStream
- [ ] Memory: tránh load toàn bộ file vào RAM
- [ ] Thumbnail loading: Glide cache + placeholder
- [ ] RecyclerView: DiffUtil + ViewHolder recycling
- [ ] Coroutines: Dispatchers.IO cho network/file operations

#### Bước 5.4: Testing
- [ ] Unit test: QrCodeHelper, NetworkUtils, FileTransferProtocol
- [ ] Integration test: HotspotManager + WifiHelper
- [ ] Manual test: truyền file giữa 2 thiết bị thật
  - Test các loại file: ảnh, video, nhạc, APK, documents
  - Test file lớn (>500MB)
  - Test nhiều file cùng lúc (>50 files)
  - Test khi mất WiFi giữa chừng
  - Test khi app bị kill
- [ ] Test trên các thiết bị: Samsung, Xiaomi, Oppo, Pixel (khác behavior WiFi/Hotspot)

#### Bước 5.5: Final Integration
- [ ] Review toàn bộ strings.xml — đảm bảo không hardcode text
- [ ] Multi-language support cho strings mới
- [ ] ProGuard rules cho ZXing, CameraX, ML Kit
- [ ] Kiểm tra memory leaks (LeakCanary)
- [ ] Final UI review: khớp với design mockup

#### ✅ Milestone Phase 5: App hoàn chỉnh, sẵn sàng release

---

## 9. Open Questions

> **Q1**: Entry point cho Send/Receive từ Home screen là gì?
> - Thêm 2 nút Send/Receive riêng vào Quick Tools?
> - Dùng nút FAB(+) → bottom sheet chọn Send/Receive?
> - Hay tạo màn hình trung gian?

> **Q2**: Có cần hỗ trợ cross-platform (gửi/nhận với iOS) không?

> **Q3**: Có muốn implement Group Share (gửi cho nhiều thiết bị cùng lúc) không?

> **Q4**: File nhận được lưu ở đâu? Thư mục riêng của app hay thư mục chung (Downloads/ShareFile/)?

> **Q5**: Cần hỗ trợ gửi file từ bên ngoài app (Share Intent từ Gallery, File Manager...) không?

---

## Tổng Kết

| Hạng mục | Số lượng |
|----------|---------|
| Tổng màn hình | 11 |
| Activities mới | 8 |
| Fragments mới | 6 |
| Dialogs mới | 2 |
| Services mới | 3 |
| Utility classes | 5 |
| Room entities | 1 |
| Layouts mới (ước tính) | ~25 |
| Thời gian ước tính | 5-6 tuần |
