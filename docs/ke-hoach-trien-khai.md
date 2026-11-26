# Kế hoạch Triển khai Tổng thể (Master Plan): Portal Đặt bàn Online & Vận hành Đa Nhà hàng

> Tài liệu đặc tả kiến trúc, nghiệp vụ thực tế, cơ sở dữ liệu, quy tắc rào lỗi (edge-case guardrails) và phân rã công việc (WBS) cho nhóm 5 thành viên (**K1–K5**), đồng bộ với trang **Project cuối (HK4)** trên Notion và sơ đồ [erd.mmd](erd.mmd).

---

## 1. Định hướng Sản phẩm & Công nghệ Đã Chốt

- **Mô hình sản phẩm:** Nền tảng (Portal/Marketplace) kết nối thực khách với nhiều nhà hàng/chuỗi thương hiệu ẩm thực (tương tự sự kết hợp giữa **PasGo** ở phía khám phá/ưu đãi/đặt bàn và **TableCheck / iPOS** ở phía điều phối sơ đồ bàn tại nhà hàng).
- **Lộ trình đa nền tảng (Dùng chung 100% Backend API & Realtime):**
  1. **Giai đoạn 1 — Website (React + Vite):** Đầy đủ 3 phân khu: **Cổng Khách hàng (Public & Customer Portal)**, **Cổng Quản lý & Vận hành Nhà hàng (Owner & Staff Backoffice)** và **Cổng Quản trị Sàn (Platform Admin Console)**.
  2. **Giai đoạn 2 — Mobile App (Flutter):** Triển khai đầy đủ cả **Phân hệ Khách hàng** (tìm nhà hàng GPS gần tôi, xem menu/ưu đãi, đặt bàn & đặt món trước, thanh toán QR cọc, ví mã QR check-in, đánh giá) **VÀ Phân hệ Quản lý Nhà hàng** (sơ đồ bàn trực quan, quét camera QR check-in 1 chạm, hàng chờ walk-in, cấu hình ca/bàn/menu, xem sổ cái đối soát & báo cáo). Admin sàn vận hành trên Web.
- **Ngăn xếp công nghệ (Tech Stack) thống nhất:**
  - **Backend:** Java 21 + Spring Boot 3.x (`Spring Web`, `Spring Data JPA`, `Spring Security`, `Bean Validation`, `Spring WebSocket/STOMP`, `Spring Mail`, `Spring Scheduling`).
  - **Cơ sở dữ liệu & Migration:** **MySQL 8.x (InnoDB)** + **Flyway** quản lý phiên bản schema và seed data trong Git.
  - **Xác thực & Phân quyền:** **Stateless JWT** (`Access Token` 15–30 phút + `Refresh Token` lưu bảng `REFRESH_TOKENS` hỗ trợ thu hồi phiên tức thì) dùng chung đồng nhất cho cả React và Flutter. Quyền theo từng nhà hàng (`OWNER`/`STAFF`) kiểm tra động tại DB qua Custom Guard.
  - **Tích hợp bên thứ ba (Miễn phí/Sandbox thân thiện thực tế Việt Nam):**
    - **Thanh toán tiền cọc:** **PayOS (VietQR tự động bắt biến động qua Webhook)** làm luồng chính + **VNPay Sandbox** dự phòng.
    - **Lưu trữ & tối ưu ảnh:** **Cloudinary** (tự động nén WebP, lưu `public_id`).
    - **Bản đồ & Vị trí:** **OpenStreetMap** (`Leaflet` trên React, `flutter_map` trên Flutter, kết hợp Goong/Nominatim geocoding + công thức Haversine/Spatial lọc quán gần tôi).
    - **Thông báo Realtime & Push:** **WebSocket (STOMP)** khi đang mở Web/App + **Firebase Cloud Messaging (FCM)** đẩy thông báo về điện thoại + **Email bất đồng bộ (`@Async`)** gửi mã đặt bàn kèm ảnh QR.
  - **Chuẩn thời gian:** Cố định múi giờ xử lý nghiệp vụ về `Asia/Ho_Chi_Minh` (`UTC+07:00`); mọi payload API dùng chuẩn ISO-8601 có offset (`2026-11-20T19:00:00+07:00`).

---

## 2. Quy tắc Nghiệp vụ Thực tế (Đã chốt dứt điểm)

### 2.1. Thương hiệu, Hồ sơ Nhà hàng, Thực đơn & Ưu đãi
1. **Thương hiệu & Chi nhánh (`BRANDS` & `RESTAURANTS`):** Một nhà hàng có thể hoạt động độc lập (`brand_id = NULL`) hoặc thuộc một chuỗi thương hiệu (ví dụ chuỗi Gogi House có nhiều chi nhánh; mỗi bản ghi `RESTAURANTS` là một địa điểm vật lý có tọa độ `latitude/longitude`, sơ đồ bàn, lịch mở cửa và sổ cái đối soát riêng).
2. **Duyệt & Vòng đời Nhà hàng:** Chủ quán đăng ký nhà hàng mới (`PENDING_APPROVAL`, gắn quyền `OWNER` trong cùng transaction) → Cấu hình đủ giờ mở cửa và ít nhất 1 bàn hoạt động → Admin duyệt `ACTIVE` mới hiển thị công khai và nhận đặt bàn. Nếu bị `REJECTED` (kèm lý do `status_reason`), chủ quán được sửa hồ sơ và bấm gửi duyệt lại (`PENDING_APPROVAL`).
3. **Thực đơn & Đặt món trước (`MENU_CATEGORIES`, `MENU_ITEMS`, `RESERVATION_ITEMS`):** Nhà hàng đăng danh mục món ăn, giá niêm yết, đánh dấu món đặc sắc (`is_signature`) và tải ảnh tờ menu (`image_type = MENU`). Khi đặt bàn, khách có thể **tùy chọn chọn trước món ăn (`Pre-order`)** để nhà hàng chuẩn bị sẵn (snapshot tên món & đơn giá tại thời điểm đặt).
4. **Ưu đãi theo khung giờ (`PROMOTIONS`):** Nhà hàng tự tạo chương trình ưu đãi đặt bàn (giảm `%`, giảm tiền mặt hoặc tặng món) giới hạn theo ngày trong tuần, khung giờ vàng/thấp điểm (VD `11:00–13:30`) và số khách tối thiểu để thu hút khách đặt qua Portal.

### 2.2. Giờ hoạt động, Lịch Lễ Tết & Thời lượng quay vòng bàn (UX Việt Nam)
5. **Nhiều ca hoạt động trong ngày (`OPENING_HOURS`):** Do từng nhà hàng tự quyết định — quán mở thông trưa tạo 1 ca (`10:00–22:00`), quán nghỉ chiều tạo 2 ca (`10:30–14:00` và `17:00–22:00`). Các ca trong cùng ngày không được chồng lấn và `opens_at < closes_at`. Ngày nghỉ cố định hàng tuần không tạo bản ghi ca.
6. **Ghi đè ngày Lễ / Đóng cửa đột xuất (`SPECIAL_SCHEDULES`):** Nhà hàng cấu hình ngày cụ thể (`specific_date`) để báo đóng cửa nghỉ Tết/sửa quán (`is_closed = true`), thay đổi giờ mở/đóng, hoặc bật cờ **bắt buộc đặt cọc ngày Lễ (`force_deposit = true`)**. Khi kiểm tra giờ mở cửa của ngày `D`, hệ thống luôn ưu tiên `SPECIAL_SCHEDULES` trước rồi mới fallback về `OPENING_HOURS`.
7. **Thời lượng dùng bàn ngầm (`estimated_turn_minutes`):**
   - **Phía Khách hàng:** Chỉ chọn **Ngày** và **Giờ đến (`starts_at`)** theo bước nhảy `slot_step_minutes` (`15` hoặc `30 phút`, ví dụ `18:30`, `19:00`). **Ẩn giờ kết thúc** để khách thoải mái dùng bữa đúng văn hóa Việt Nam (chỉ hiển thị ghi chú giới hạn thời gian nếu nhà hàng bật `show_turn_limit_note = true` cho mô hình Buffet).
   - **Phía Hệ thống & Chủ quán:** Chủ quán tự cấu hình `estimated_turn_minutes` (VD cơm/mì `60p`, lẩu nướng `120p`, hải sản/nhậu `180p`) để hệ thống tính `ends_at = starts_at + estimated_turn_minutes` phục vụ xếp lịch ca sau. Nhân viên tại quầy có quyền gia hạn `ends_at` bất kỳ lúc nào.

### 2.3. Xếp bàn, Ghép bàn, Đổi bàn & Chống lãng phí ghế
8. **Bảng nối đa bàn (`RESERVATION_TABLES`):**
   - **Đặt Online thông thường:** Hệ thống tự động chọn **1 bàn hoạt động tối ưu nhất** thỏa mãn `table.min_party_size <= guest_count <= table.capacity` (ưu tiên `capacity ASC, id ASC`). Quy tắc `min_party_size` ngăn chặn việc 1–2 khách đặt online tự động chiếm phòng VIP 10–12 người.
   - **Đoàn đông & Ghép bàn / Đổi bàn:** Nếu số khách vượt bàn đơn lớn nhất, hệ thống có thể ghép các bàn có `is_combinable = true` cùng khu vực (`area_name`) hoặc chuyển sang chế độ chờ nhà hàng duyệt/xếp bàn (`PENDING_APPROVAL`). Lễ tân trên màn hình vận hành (Web/Flutter) có thể **Ghép 2+ bàn cho 1 đơn** hoặc **Đổi bàn (`Reassign`)** khi bàn trước ngồi lố giờ mà không làm thay đổi mã `booking_code` của khách.
   - **Khách tự sửa đơn:** Trước hạn chót `free_cancel_cutoff_minutes`, khách được đổi giờ đến hoặc số khách (hệ thống khóa và kiểm tra lại bàn trống ngay trong transaction).

### 2.4. Chế độ Xác nhận, Tiền cọc, Đối soát & Uy tín Khách hàng
9. **Ba chế độ xác nhận linh hoạt theo cấu hình từng nhà hàng:**
   - **Tự động xác nhận (`AUTO`, không cọc):** Giữ được bàn là vào `CONFIRMED` ngay.
   - **Duyệt thủ công (`MANUAL`):** Giữ bàn tạm ở `PENDING_APPROVAL` đến `approval_expires_at`; nhân viên xác nhận chuyển sang `CONFIRMED` hoặc từ chối sang `REJECTED`.
   - **Yêu cầu đặt cọc giữ bàn (`DEPOSIT`):** Áp dụng khi nhà hàng bật `deposit_policy` (`ALWAYS`, `LARGE_PARTY` khi `guest_count >= deposit_min_guests`, `PREORDER_ONLY` khi có đặt món trước), khi rơi vào ngày Lễ có `SPECIAL_SCHEDULES.force_deposit = true`, **HOẶC khi tài khoản khách có `no_show_count_30d >= 3` (`force_deposit_flag = true`)**.
10. **Luồng Tiền cọc tập trung qua Sàn & Sổ cái Đối soát:**
    - Đơn cần cọc khởi tạo ở `PENDING_PAYMENT`, giữ bàn tạm thời trong **15 phút** (`payment_expires_at = NOW() + 15p`).
    - Khách quét mã VietQR (PayOS) hoặc thanh toán VNPay về tài khoản cổng thanh toán tập trung của Portal → Webhook xác nhận thành công chuyển đơn sang `CONFIRMED` và sinh mã QR check-in. Quá 15 phút chưa thanh toán → Cronjob chuyển sang `EXPIRED`, giải phóng bàn ngay.
    - **Quy tắc Hoàn cọc & Đền bù:**
      - Khi đơn `COMPLETED`: Tiền cọc được giải ngân cộng vào số dư sổ cái của nhà hàng (`RESTAURANT_LEDGER_ENTRIES`, trừ phí nền tảng nếu cấu hình) để nhà hàng cấn trừ vào hóa đơn ăn uống tại quầy và rút đối soát định kỳ (`SETTLEMENT_PAYOUTS`).
      - Khi khách hủy **trước** `free_cancel_cutoff_minutes` (mặc định 120 phút), hoặc nhà hàng từ chối/hủy đơn, hoặc nhà hàng bị Admin tạm ngưng: Tạo bản ghi hoàn cọc (`REFUND_PENDING` → Admin/Hệ thống hoàn tiền `REFUNDED`).
      - Khi khách hủy **trễ hạn** hoặc bùng bàn (`NO_SHOW`): Khách mất cọc (`FORFEITED`), tiền cọc được hạch toán đền bù vào sổ cái cho nhà hàng.
11. **Cơ chế tự động xử lý khách bùng bàn (`No-show Trust Policy`):** Không khóa cứng tài khoản khách hàng một cách cực đoan; nếu khách bị đánh dấu `NO_SHOW >= 3` lần trong vòng 30 ngày, hệ thống tự động bật `force_deposit_flag = true` — mọi lần đặt bàn tiếp theo của khách này ở bất kỳ nhà hàng nào đều bắt buộc đặt cọc mới giữ chỗ.

### 2.5. Walk-in, Hàng chờ (`WAITLIST`), Quét QR Check-in & Đánh giá Xác thực
12. **Walk-in & Hàng chờ tại quầy:**
    - **Còn bàn trống:** Lễ tân chọn 1 hoặc nhiều bàn trên sơ đồ, bấm tạo `WALK_IN` (`status = CHECKED_IN` ngay lập tức, `contact_name`/`contact_phone` là **tùy chọn** để thao tác trong 3 giây).
    - **Hết bàn trống:** Lễ tân thêm khách vào `WAITLIST_ENTRIES` (**bắt buộc** Tên + SĐT + Số khách), cấp phiếu số thứ tự (`Q001, Q002...`). Ngay khi có bàn bấm `COMPLETED`, màn hình lễ tân tự động gợi ý khách đang chờ có số người phù hợp để bấm **"Gọi khách & Xếp bàn"** (chuyển thành lượt `WALK_IN` `CHECKED_IN`).
13. **Quét mã QR Check-in:** Mỗi đơn `CONFIRMED` có `qr_token` duy nhất hiển thị trên Web/App Flutter và trong Email. Khi khách đến cửa quán, lễ tân dùng App Flutter nhà hàng (hoặc webcam Web) quét QR để định danh đơn và bấm `CHECKED_IN` trong 1 chạm.
14. **Đánh giá xác thực (`REVIEWS`):** Chỉ khách hàng có đơn ở trạng thái `COMPLETED` mới được gửi tối đa **1 bài đánh giá** cho đơn đó (`UNIQUE(reservation_id)`), chấm điểm Đồ ăn / Không gian / Phục vụ (1–5 sao) kèm tối đa 4 ảnh thực tế; Chủ nhà hàng được phép phản hồi (`owner_reply`).

---

## 3. Thiết kế Cơ sở Dữ liệu (24 Bảng Chuẩn hóa)

Sơ đồ chi tiết đầy đủ các cột và quan hệ nằm trong [erd.mmd](erd.mmd). Dưới đây là phân bổ 24 bảng theo 5 phân hệ sở hữu:

| Phân hệ | Các bảng thuộc phân hệ | Thành viên sở hữu chính |
| --- | --- | --- |
| **1. Tài khoản, Bảo mật & Thông báo** (4 bảng) | `USERS`, `REFRESH_TOKENS`, `USER_DEVICES`, `NOTIFICATIONS` | **K1** |
| **2. Thương hiệu, Nhà hàng, Menu & Ưu đãi** (6 bảng) | `BRANDS`, `RESTAURANTS`, `RESTAURANT_IMAGES`, `MENU_CATEGORIES`, `MENU_ITEMS`, `PROMOTIONS` | **K2** |
| **3. Động cơ Đặt bàn, Ghép bàn & Đặt món trước** (4 bảng lõi) | `RESERVATIONS`, `RESERVATION_TABLES`, `RESERVATION_ITEMS`, `RESERVATION_STATUS_HISTORY` | **K3** |
| **4. Vận hành: Nhân sự, Lịch/Lễ, Sơ đồ bàn & Hàng chờ** (6 bảng) | `RESTAURANT_MEMBERS`, `STAFF_INVITATIONS`, `OPENING_HOURS`, `SPECIAL_SCHEDULES`, `DINING_TABLES`, `WAITLIST_ENTRIES` | **K4** |
| **5. Thanh toán Cọc, Đối soát, Đánh giá & Quản trị** (4 bảng) | `PAYMENT_TRANSACTIONS`, `RESTAURANT_LEDGER_ENTRIES`, `SETTLEMENT_PAYOUTS`, `REVIEWS` | **K5** |

### Các Ràng buộc Toàn vẹn Cấp độ Database (Bắt buộc trong Flyway `V1`)
- **Chống lệch nhà hàng chéo bảng (Cross-tenant Composite Foreign Keys):**
  - Tạo `UNIQUE KEY uk_reservations_rest_id (restaurant_id, id)` trên `RESERVATIONS`.
  - Tạo `UNIQUE KEY uk_tables_rest_id (restaurant_id, id)` trên `DINING_TABLES`.
  - Tại `RESERVATION_TABLES`, khai báo 2 khóa ngoại ghép: `(restaurant_id, reservation_id) -> RESERVATIONS(restaurant_id, id)` và `(restaurant_id, table_id) -> DINING_TABLES(restaurant_id, id)`. Nhờ vậy Database chặn tuyệt đối 100% lỗi ghép nhầm bàn của nhà hàng B vào đơn của nhà hàng A!
  - Tương tự cho `MENU_ITEMS(restaurant_id, category_id)` và `RESERVATIONS(restaurant_id, promotion_id)`.
- **Không xóa vật lý (No Hard Delete) dữ liệu có lịch sử:** Dùng cờ `status` / `is_active` thay cho `DELETE` đối với `USERS`, `RESTAURANTS`, `DINING_TABLES`, `MENU_ITEMS`.
- **Snapshot dữ liệu giao dịch:** `RESERVATIONS` lưu cứng `contact_name`, `contact_phone`, `ends_at`, `deposit_required_amount`; `RESERVATION_ITEMS` lưu cứng `item_name_snapshot` và `unit_price_snapshot` để khi nhà hàng đổi cấu hình/giá món không làm sai lệch lịch sử đơn cũ.
- **Indexes trọng yếu:**
  - `RESERVATIONS (restaurant_id, status, starts_at, ends_at)`
  - `RESERVATIONS (status, payment_expires_at)` và `(status, starts_at)` *(phục vụ Cronjob quét đơn hết hạn thanh toán / No-show siêu nhanh)*
  - `RESERVATION_TABLES (table_id, reservation_id)`
  - `RESTAURANTS (status, city, district, cuisine_type)` và `(latitude, longitude)`
  - `WAITLIST_ENTRIES (restaurant_id, status, created_at)`

---

## 4. Thuật toán Bàn trống, Khóa Chống Trùng & Xử lý Ngồi Lố Giờ (`Overstay`)

### 4.1. Công thức Kiểm tra Bàn Bận (Active Occupancy)
Một bàn `T` bị coi là đang bận trong khoảng yêu cầu `[req_start, req_end)` nếu tồn tại bản ghi `existing` trong `RESERVATIONS` nối qua `RESERVATION_TABLES` vào bàn `T` thỏa mãn **đồng thời**:

1. **Trạng thái còn hiệu lực giữ bàn:**
   - `existing.status IN ('CONFIRMED', 'CHECKED_IN', 'PENDING_APPROVAL')`
   - **HOẶC** `(existing.status = 'PENDING_PAYMENT' AND existing.payment_expires_at > NOW())`
2. **Giao khoảng thời gian (có tính khách đang ngồi lố giờ thực tế):**
   - Định nghĩa mốc kết thúc hiệu lực của đơn đang giữ bàn:
     - Nếu `existing.status = 'CHECKED_IN'` (khách vẫn chưa đứng dậy): `effective_end = GREATEST(existing.ends_at, NOW() + INTERVAL 15 MINUTE)`
     - Các trạng thái còn lại: `effective_end = existing.ends_at`
   - Điều kiện xung đột:
     ```sql
     existing.starts_at < :reqEnd
     AND effective_end > :reqStart
     ```
   *(Đơn kết thúc đúng `19:00` không xung đột với đơn bắt đầu lúc `19:00` nếu khách trước đã rời bàn đúng giờ).*

### 4.2. Quy trình Khóa Bi Quan Chống Race Condition & Chống Deadlock (MySQL InnoDB)
Để ngăn chặn tuyệt đối 2 request đặt trùng bàn cuối cùng VÀ tránh lỗi đọc Snapshot cũ của `REPEATABLE READ` hay Deadlock khi ghép nhiều bàn:

1. Đặt `@Transactional(isolation = Isolation.READ_COMMITTED)` và `@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")` trên Service tạo/sửa/đổi bàn của K3.
2. Validate nhà hàng `ACTIVE`, kiểm tra lịch ngày (`SPECIAL_SCHEDULES` → `OPENING_HOURS`), kiểm tra giới hạn chống spam của khách.
3. **Với Đặt bàn Online tự động:**
   - Truy vấn lọc nhanh danh sách bàn ứng viên đang `is_active = true` đủ sức chứa (`min_party_size <= guest_count <= capacity`), sắp xếp `capacity ASC, id ASC`.
   - Duyệt ứng viên: Khóa dòng `DINING_TABLES` bằng `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`), sau đó chạy ngay truy vấn kiểm tra xung đột mục `4.1`. Nếu trống → tạo `RESERVATIONS`, `RESERVATION_TABLES`, `RESERVATION_STATUS_HISTORY` (và `PAYMENT_TRANSACTIONS` nếu cần cọc) → Commit và bắn sự kiện WebSocket/Email. Nếu vừa bị luồng khác chiếm → thử bàn ứng viên tiếp theo.
4. **Với Ghép nhiều bàn / Lễ tân chọn bàn cụ thể / Đổi bàn:**
   - Tập hợp toàn bộ `table_ids` liên quan (cả bàn cũ lẫn bàn mới nếu là Đổi bàn), **bắt buộc sắp xếp tăng dần `ORDER BY id ASC`** trước khi thực thi `SELECT ... FOR UPDATE WHERE id IN (:sortedTableIds)`. Quy tắc khóa theo thứ tự ID tăng dần triệt tiêu **100% nguy cơ Deadlock** giữa hai nhân viên thao tác chéo bàn cùng lúc!

### 4.3. Vòng đời Trạng thái Đơn Đặt bàn

```text
                       ┌──(Quá 15p chưa trả cọc)──► EXPIRED
                       │
Tạo ONLINE ─┬─► PENDING_PAYMENT ──(Webhook PAID)──► CONFIRMED
            ├─► PENDING_APPROVAL ─┬─(Duyệt)───────► CONFIRMED
            │                     └─(Từ chối)─────► REJECTED
            └─► CONFIRMED (Auto, 0đ cọc)
                    ├──(Khách/Quán/Hệ thống hủy)──► CANCELLED
                    ├──(Quá giờ + ân hạn)─────────► NO_SHOW
                    └──(Quét QR / Lễ tân nhận)────► CHECKED_IN ──(Rời bàn)──► COMPLETED ──► [Mở quyền REVIEW]

Tạo WALK_IN (hoặc từ WAITLIST) ───────────────────► CHECKED_IN ──(Rời bàn)──► COMPLETED
```

---

## 5. Ma trận Rào 32 Trường hợp Biên (Edge-Case & Guardrails Matrix)

Mọi thành viên phải kiểm tra danh sách này khi viết Service Validation và Test Cases:

| Mã | Tình huống thực tế / Lỗ hổng tiềm ẩn | Quy tắc rào cứng tại Backend / DB |
| --- | --- | --- |
| **E01** | Khách gửi `starts_at` ở quá khứ hoặc sát giờ (`18:55` đặt `19:00`) | Chặn `starts_at < NOW() + restaurant.min_advance_minutes` (mặc định 30p) với nguồn `ONLINE`. |
| **E02** | Khách gọi thẳng API gửi giờ lẻ (`18:07:23`) làm vỡ lịch slot | Bắt buộc số phút của `starts_at` chia hết cho `slot_step_minutes` (`15` hoặc `30`) và giây = `00`. |
| **E03** | Khách đặt bàn quá xa (VD năm sau) | Chặn `starts_at > NOW() + restaurant.max_advance_days` (mặc định 30 ngày). |
| **E04** | Quán nghỉ giữa ca (`14:00–17:00`), khách đặt `13:30` (quay vòng 90p lấn sang `15:00`) | Khoảng `[starts_at, ends_at]` của đơn Online phải nằm trọn vẹn trong **cùng một ca mở cửa** hợp lệ của ngày đó. |
| **E05** | Quán báo đóng cửa đột xuất (`SPECIAL_SCHEDULES.is_closed = true`) vào Thứ Bảy tuần tới | Chặn nhận đơn mới vào ngày đó; khi chủ quán bật đóng cửa đột xuất, hệ thống liệt kê các đơn tương lai trong ngày đó để tự động hủy + hoàn cọc 100% + gửi thông báo xin lỗi khách. |
| **E06** | Khách Walk-in vào lúc `21:15` khi `22:00` đóng cửa (cộng 90p thành `22:45`) | Với `WALK_IN`, nếu thời gian còn lại tới `closes_at >= 30 phút`, tự động co `ends_at = closes_at` thay vì từ chối. |
| **E07** | Lệch múi giờ giữa Server Cloud (`UTC`) và giờ mở cửa Việt Nam (`+07:00`) | Quy đổi toàn bộ `Instant`/`OffsetDateTime` về `ZoneId.of("Asia/Ho_Chi_Minh")` trước khi tách `day_of_week` và `LocalTime`. |
| **E08** | 20 người cùng bấm đặt chiếc bàn cuối cùng lúc `19:00` | Khóa `PESSIMISTIC_WRITE` trên `DINING_TABLES` trong `READ_COMMITTED`: đúng 1 người thành công, 19 người nhận thông báo hết bàn hoặc gợi ý khung giờ lân cận (`18:30` / `19:30`). |
| **E09** | 2 lễ tân ghép bàn chéo nhau cùng lúc (`T1+T2` và `T2+T1`) gây Deadlock | Luôn sort `tableIds` tăng dần trước khi `SELECT ... FOR UPDATE`. |
| **E10** | 1 khách lẻ đặt Online tự động chiếm phòng VIP 12 chỗ | Kiểm tra `table.min_party_size <= guest_count` khi tự xếp bàn Online; lễ tân xếp tay tại quầy thì được phép override. |
| **E11** | Khách spam đặt 10 bàn giữ chỗ ảo ở các quán miễn phí cọc | Mỗi `CUSTOMER` tối đa **3 đơn đang chờ (`PENDING_*` / `CONFIRMED`)** trong tương lai và **cấm 2 đơn của cùng 1 khách giao thời gian nhau**. |
| **E12** | Khách có lịch sử bùng bàn liên tục (`no_show_count_30d >= 3`) | Tự động bật `force_deposit_flag = true`: bắt buộc thanh toán tiền cọc mới giữ chỗ dù quán đó bình thường miễn phí cọc. |
| **E13** | Khách giữ màn hình QR PayOS không thanh toán làm treo bàn | `PENDING_PAYMENT` chỉ giữ bàn đến `payment_expires_at` (`15 phút`). Cronjob chạy mỗi 1 phút chuyển các đơn quá hạn sang `EXPIRED` và ghi lịch sử `SYSTEM`. |
| **E14** | Khách chuyển khoản PayOS ở phút thứ `15:05` ngay sau khi đơn vừa bị `EXPIRED` | Khi xử lý Webhook thanh toán: khóa dòng `RESERVATIONS`; nếu đơn đã `EXPIRED`, kiểm tra lại bàn đó còn trống không — nếu còn trống thì khôi phục `CONFIRMED`; nếu đã bị người khác đặt mất thì chuyển giao dịch sang `REFUND_PENDING` để hoàn tiền 100% cho khách! |
| **E15** | Webhook PayOS/VNPay bắn lặp lại 2–3 lần (Retry) | Kiểm tra tính bất biến (`Idempotency`) qua `order_code` và khóa dòng `PAYMENT_TRANSACTIONS`: nếu đã `PAID` thì trả `200 OK` bỏ qua, tuyệt đối không cộng sổ cái 2 lần. |
| **E16** | Khách ngồi nhậu lố giờ dự kiến (`Overstay`) đè lên đơn `CONFIRMED` sắp tới | Đơn `CHECKED_IN` tự động giữ bàn tới `GREATEST(ends_at, NOW() + 15p)`; UI lễ tân nháy đỏ cảnh báo và cung cấp nút **"Đổi sang bàn trống khác"** cho đơn `CONFIRMED` kế tiếp. |
| **E17** | Khách ăn xong rời bàn sớm 45 phút | Lễ tân bấm `COMPLETED` → cập nhật `completed_at = NOW()`, bàn được giải phóng ngay tức khắc cho khách mới. |
| **E18** | Khách bấm Hủy đơn sát giờ hẹn (VD trước 15 phút) để đòi hoàn cọc | Trước `free_cancel_cutoff_minutes`: hoàn cọc 100% (`REFUND_PENDING`). Sau mốc này: cảnh báo rõ trên UI khách sẽ mất tiền cọc (`FORFEITED`) nếu vẫn xác nhận hủy (hoặc khóa hủy online tùy cấu hình quán). |
| **E19** | Khách bấm Hủy trên App đúng lúc Lễ tân quét QR Check-in tại cửa | Khóa dòng `RESERVATIONS` khi đổi trạng thái: ai vào trước thắng, luồng sau nhận lỗi trạng thái không còn hợp lệ (`409 CONFLICT`). |
| **E20** | Lễ tân bấm nhầm `NO_SHOW` khi chưa tới giờ hẹn, hoặc `CHECKED_IN` cho đơn ngày mai | Chặn cứng ở State Machine: `CHECKED_IN` chỉ mở trong `[starts_at - 30p, ends_at)`; `NO_SHOW` chỉ mở khi `NOW() >= starts_at + grace_period_minutes` (mặc định 15p). |
| **E21** | Lễ tân quên xử lý đơn khách không đến tối qua | Cronjob mỗi 15 phút tự chuyển các đơn `CONFIRMED` quá `starts_at + 45 phút` sang `NO_SHOW` (tăng `no_show_count_30d` của khách và hạch toán cọc `FORFEITED` nếu có). |
| **E22** | Kẻ gian chụp màn hình mã `booking_code` dạng chữ dễ đoán để check-in mạo danh | Quét QR sử dụng `qr_token` ngẫu nhiên mật mã học (UUIDv4/HMAC 128-bit), chỉ hiển thị cho chính chủ đơn và chỉ dùng được 1 lần tại đúng `restaurant_id` đó. |
| **E23** | Khách ở Hàng chờ (`WAITLIST`) được gọi nhưng đã bỏ đi quán khác | Hỗ trợ trạng thái `CALLED` → quá 10 phút không vào bàn thì bấm `SKIPPED` để gọi số tiếp theo mà vẫn lưu thống kê tỷ lệ bỏ chờ. |
| **E24** | Khách chưa ăn hoặc bị hủy đơn nhưng cố gọi API gửi Review dìm hàng quán | Chặn tại Service & DB: `REVIEWS.reservation_id` phải thuộc `customer_id` đang đăng nhập, có `status = COMPLETED`, và `UNIQUE(reservation_id)` chặn đánh giá 2 lần. |
| **E25** | Nhà hàng đổi giá món từ 100k lên 150k sau khi khách đã đặt món trước (`Pre-order`) | Đơn cũ hiển thị đúng `unit_price_snapshot = 100k` đã lưu trong `RESERVATION_ITEMS`. |
| **E26** | Admin bấm duyệt `ACTIVE` nhà hàng rỗng (chưa tạo bàn hoặc chưa có giờ mở cửa) | Chặn API duyệt: bắt buộc có `>= 1` `OWNER` active, `>= 1` ca `OPENING_HOURS` và `>= 1` `DINING_TABLES` active. |
| **E27** | Chủ quán giảm `capacity` bàn hoặc tắt bàn/xóa ca mở cửa đang có đơn tương lai | Kiểm tra các đơn tương lai bị ảnh hưởng: yêu cầu Chủ quán sử dụng chức năng **Đổi bàn** chuyển các đơn đó sang bàn hợp lệ khác trước khi lưu thay đổi cấu hình. |
| **E28** | Chủ quán tự xóa hoặc hạ quyền `OWNER` cuối cùng của nhà hàng | Khóa danh sách `RESTAURANT_MEMBERS` của nhà hàng đó và chặn thao tác nếu số `OWNER` active còn lại `< 1`. |
| **E29** | Nhân viên vừa bị chủ quán cho nghỉ (`INACTIVE`) nhưng JWT Access Token vẫn còn hạn 15 phút | `RestaurantSecurityGuard` truy vấn trạng thái `RESTAURANT_MEMBERS.status = ACTIVE` trực tiếp (hoặc qua cache ngắn xóa tức thì khi đổi quyền), chặn truy cập ngay lập tức. |
| **E30** | Admin khóa tài khoản `LOCKED` một kẻ phá hoại đang giữ bàn tối nay | Thu hồi toàn bộ `REFRESH_TOKENS`, chặn Filter xác thực và tự động hủy các đơn tương lai chưa thanh toán/miễn phí để trả bàn cho nhà hàng. |
| **E31** | Admin `SUSPENDED` một nhà hàng vi phạm đang có đơn cọc tương lai | Ẩn nhà hàng khỏi tìm kiếm ngay lập tức, chặn đặt mới, tự động hủy các đơn tương lai kèm chuyển giao dịch cọc sang `REFUND_PENDING` 100% cho khách. |
| **E32** | Nhà hàng bấm rút đối soát (`SETTLEMENT_PAYOUTS`) vượt quá số dư khả dụng hoặc bấm đúp 2 lần | Khóa bi quan số dư sổ cái của nhà hàng trước khi tạo yêu cầu rút; khấu trừ tạm giữ ngay vào `RESTAURANT_LEDGER_ENTRIES` để không thể rút trùng. |

---

## 6. Kiến trúc Phân quyền Đa Tầng

| Actor | Phạm vi | Quyền hạn cốt lõi (Áp dụng đồng nhất trên cả Web React & App Flutter) |
| --- | --- | --- |
| **Guest** *(Chưa đăng nhập)* | Công khai | Tìm kiếm, lọc nhà hàng `ACTIVE` (từ khóa, khu vực, bản đồ gần tôi, ẩm thực, giá, còn bàn trống), xem chi tiết, menu, ưu đãi, đánh giá và khung giờ trống. |
| **CUSTOMER** | Cá nhân (`USER`) | Đặt bàn & đặt món trước, áp mã ưu đãi khung giờ, thanh toán QR cọc, xem mã QR check-in, sửa/hủy đơn trước hạn, nhận thông báo, viết đánh giá cho đơn `COMPLETED`, đăng ký mở nhà hàng mới. |
| **STAFF** | Theo nhà hàng (`RESTAURANT_MEMBERS`) | Xem sơ đồ/lịch bàn realtime, quét camera QR check-in, ghi nhận `WALK_IN`, quản lý hàng chờ `WAITLIST`, ghép/đổi bàn, gia hạn giờ ngồi, hoàn tất/hủy/đánh dấu `NO_SHOW`, duyệt đơn `MANUAL`, cập nhật món hết hàng trong ngày. |
| **OWNER** | Theo nhà hàng (`RESTAURANT_MEMBERS`) | Toàn bộ quyền của `STAFF` + quản lý thương hiệu/hồ sơ/ảnh nhà hàng, thực đơn, chương trình ưu đãi, ca mở cửa & lịch Lễ Tết, cấu hình sơ đồ bàn, cấu hình chính sách đặt cọc, mời/quản lý nhân viên, phản hồi đánh giá, xem báo cáo & gửi yêu cầu rút tiền đối soát. |
| **ADMIN** | Toàn nền tảng (`ADMIN`, dùng Web) | Duyệt/từ chối/tạm ngưng nhà hàng, khóa/mở tài khoản, quản lý giao dịch thanh toán cọc, xử lý hoàn tiền (`REFUNDS`), duyệt chi trả đối soát (`SETTLEMENT_PAYOUTS`), kiểm duyệt đánh giá vi phạm, xem thống kê toàn sàn. |

---

## 7. Danh mục Nhóm API Chuẩn hóa (REST `/api/v1` + WebSocket)

Mọi tính năng của Khách hàng và Nhà hàng đều giao tiếp qua JSON DTO thống nhất để App Flutter ở giai đoạn 2 tái sử dụng nguyên vẹn 100%:

- **Auth & Profile (`K1`):** `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `GET/PATCH /me`, `POST /me/devices` *(đăng ký FCM token)*, `GET/PATCH /me/notifications`.
- **Discovery, Brands, Menu & Promo Public (`K2`):** `GET /brands`, `GET /restaurants` *(lọc đa tiêu chí + tọa độ `lat,lng,radiusKm` + kiểm tra còn chỗ)*, `GET /restaurants/{slugOrId}`, `GET /restaurants/{id}/menu`, `GET /restaurants/{id}/promotions`, `GET /restaurants/{id}/reviews`.
- **Restaurant Profile, Menu & Promo Management (`K2`):** `POST /restaurants`, `PATCH /management/restaurants/{id}`, `/resubmit`, CRUD `/images`, CRUD `/menu-categories` & `/menu-items`, CRUD `/promotions`.
- **Booking Engine & Customer Reservations (`K3`):** `GET /restaurants/{id}/availability` *(trả về danh sách slot hợp lệ + yêu cầu cọc dự kiến + ưu đãi khả dụng)*, `POST /reservations`, `GET /me/reservations`, `GET /me/reservations/{id}`, `PATCH /reservations/{id}` *(khách đổi giờ/số người)*, `POST /reservations/{id}/cancel`.
- **Restaurant Operations, Tables, Shifts, Staff & Waitlist (`K4` phối hợp `K3`):**
  - Cấu hình: CRUD `/management/restaurants/{id}/tables` *(kèm tọa độ `pos_x, pos_y`)*, `/opening-hours`, `/special-schedules`, `/members`, `/invitations` (`POST /invitations/{token}/accept`).
  - Điều phối trong ngày: `GET /management/restaurants/{id}/live-board`, `POST .../reservations/walk-ins`, `POST .../reservations/check-in-by-qr`, các action `/approve`, `/reject`, `/check-in`, `/extend`, `/reassign-tables`, `/complete`, `/no-show`, `/cancel`, và CRUD `/waitlist` (`/call`, `/seat`, `/skip`).
- **Payments, Ledger, Reviews & Platform Admin (`K5`):**
  - Thanh toán & Đánh giá: `POST /payments/reservations/{id}/create-link`, `POST /webhooks/payos`, `GET /webhooks/vnpay-ipn`, `POST /reservations/{id}/review`, `POST /management/reviews/{id}/reply`.
  - Đối soát nhà hàng: `GET /management/restaurants/{id}/ledger`, `POST .../payouts`, `GET .../analytics`.
  - Admin sàn: `/admin/restaurants` *(approve/reject/suspend)*, `/admin/users` *(lock/unlock)*, `/admin/refunds`, `/admin/payouts`, `/admin/reviews`, `/admin/analytics`.
- **WebSocket Topics (`K1` dựng hạ tầng):** `/topic/restaurants/{id}/board` *(đồng bộ tức thì sơ đồ bàn & đơn mới giữa Web và App Flutter của lễ tân)* và `/user/queue/notifications` *(báo trạng thái thanh toán cọc / duyệt đơn cho khách)*.

---

## 8. Phân công 5 Thành viên (`K1–K5`) & Lộ trình Chia Task (`WBS`)

### 8.1. Bảng Phân vai Cân bằng Khối lượng Công việc

| Thành viên | Vai trò & Thế mạnh | Trách nhiệm xuyên suốt (Backend + Web React + App Flutter giai đoạn 2) |
| --- | --- | --- |
| **K1** | **Tech Lead — IAM, Security & Realtime Infrastructure** *(Chắc Spring Boot & Kiến trúc)* | Dựng khung Spring Boot + Flyway + Docker, JWT + Refresh Token, `@RequireRestaurantRole`, Cloudinary helper, Mail helper, WebSocket STOMP + FCM Push, bảng `USERS`, `REFRESH_TOKENS`, `USER_DEVICES`, `NOTIFICATIONS`; dựng khung kiến trúc cho cả React và Flutter. |
| **K2** | **Discovery, Brand, Restaurant, Menu & Promotion Owner** *(Mạnh UI/UX & Search/Filter)* | Sở hữu 6 bảng (`BRANDS`, `RESTAURANTS`, `RESTAURANT_IMAGES`, `MENU_CATEGORIES`, `MENU_ITEMS`, `PROMOTIONS`); Trang chủ, Tìm kiếm nâng cao & Bản đồ quán gần tôi, Chi tiết nhà hàng, Quản lý hồ sơ quán, Thực đơn & Chương trình ưu đãi trên Web và Flutter. |
| **K3** | **Core Booking Engine, Table Allocation & Pre-order Owner** *(Chắc DB, Transaction & Thuật toán)* | Sở hữu 4 bảng lõi (`RESERVATIONS`, `RESERVATION_TABLES`, `RESERVATION_ITEMS`, `RESERVATION_STATUS_HISTORY`); Thuật toán tính slot trống, tự xếp bàn & ghép bàn, khóa bi quan chống trùng/deadlock, đặt món trước, sửa/hủy đơn, tạo/quét `qr_token`, Cronjobs (`EXPIRED` 15p & `NO_SHOW` 45p), kiểm thử đa luồng. |
| **K4** | **Restaurant Operations, Floor Plan, Schedule & Waitlist Owner** *(Mạnh tương tác vận hành)* | Sở hữu 6 bảng vận hành (`RESTAURANT_MEMBERS`, `STAFF_INVITATIONS`, `OPENING_HOURS`, `SPECIAL_SCHEDULES`, `DINING_TABLES`, `WAITLIST_ENTRIES`); Sơ đồ bàn trực quan, cấu hình nhiều ca & lịch Lễ Tết, mời nhân viên qua Email, Hàng chờ tại quầy, Màn hình Điều phối Lịch bàn Realtime & Quét camera QR Check-in trên Web và Flutter. |
| **K5** | **Fintech (PayOS/VNPay & Ledger), Verified Reviews & Admin Owner** *(Cẩn thận nghiệp vụ tiền & tích hợp)* | Sở hữu 4 bảng (`PAYMENT_TRANSACTIONS`, `RESTAURANT_LEDGER_ENTRIES`, `SETTLEMENT_PAYOUTS`, `REVIEWS`); Tích hợp VietQR PayOS & VNPay Sandbox, xử lý hoàn cọc & sổ cái đối soát nhà hàng, Đánh giá xác thực 1-1, toàn bộ cổng Admin sàn, thống kê/báo cáo nhà hàng & sàn, dữ liệu Demo & Deploy. |

### 8.2. Lộ trình Triển khai theo 5 Mốc (Tháo gỡ hoàn toàn phụ thuộc chéo)

> **Nguyên tắc vàng tuần 1:** Toàn bộ 24 bảng trong [erd.mmd](erd.mmd) và bộ Seed Data chuẩn phải được K1 + K3 + K5 khởi tạo xong ngay ở **Phase 1**. Nhờ có sẵn nhà hàng, bàn, menu và tài khoản mẫu trong DB ngay từ ngày thứ 3, cả 5 thành viên code song song độc lập mà không ai phải ngồi chờ màn hình của người khác!

#### Phase 1: Khởi tạo Nền tảng, Schema 24 Bảng & Khung Xác thực (`Mốc Nền móng`)
- **`P1-T1` (K1):** Dựng repo Backend Spring Boot + Docker Compose MySQL 8 + cấu hình Timezone `+07:00` + chuẩn hóa `ApiResponse`, `PageResponse`, mã lỗi nghiệp vụ, Swagger OpenAPI, Cloudinary & Mail service dùng chung.
- **`P1-T2` (K3 + K1):** Viết Flyway `V1__init_full_schema.sql` đủ 24 bảng, toàn bộ Composite Foreign Keys chống lệch nhà hàng và Indexes.
- **`P1-T3` (K5):** Viết `V2__seed_demo_ecosystem.sql` (2 chuỗi thương hiệu, 5 nhà hàng đủ thể loại Buffet/Lẩu/Fine Dining, menu, ưu đãi, sơ đồ bàn, lịch ca và các tài khoản `admin`, `owner`, `staff`, `customer`).
- **`P1-T4` (K1):** Hoàn thành Auth JWT + Refresh Token + Guard kiểm tra quyền theo nhà hàng + hạ tầng WebSocket & Notification.
- **`P1-T5` (K1 + K2):** Dựng khung React (Vite + Router + Axios Refresh Interceptor + 3 Layout: Public, Restaurant Backoffice, Admin Console).

#### Phase 2: Luồng Đặt bàn & Vận hành Bàn Xuyên suốt (`Core End-to-End`)
- **`P2-T1` (K2):** API & UI Đăng ký nhà hàng, Quản lý hồ sơ/ảnh/thương hiệu, Trang danh sách & chi tiết nhà hàng công khai (tích hợp bản đồ tọa độ & tìm kiếm lọc đa tiêu chí).
- **`P2-T2` (K4):** API & UI Quản lý Sơ đồ bàn (`DINING_TABLES`), Ca mở cửa nhiều ca/ngày (`OPENING_HOURS`), Lịch ngày Lễ/Đóng cửa đột xuất (`SPECIAL_SCHEDULES`) và Mời nhân viên qua Email (`STAFF_INVITATIONS`).
- **`P2-T3` (K3):** API tính slot trống `availability` + Động cơ đặt bàn `ONLINE` (tự xếp bàn theo `min_party_size`, khóa `PESSIMISTIC_WRITE` chống trùng, kiểm tra chống spam & uy tín `no_show_count_30d`) + টেস্ট đồng thời 20 luồng.
- **`P2-T4` (K3 + K4):** API & UI Màn hình Điều phối Lịch bàn Realtime trong ngày: tạo `WALK_IN`, quét `qr_token` Check-in, Ghép nhiều bàn, Đổi bàn khi `Overstay`, Gia hạn giờ, Hoàn tất, No-show + Hàng chờ `WAITLIST` tại quầy.
- **`P2-T5` (K5):** API & UI Admin duyệt/từ chối/tạm ngưng nhà hàng (kèm kiểm tra điều kiện đủ bàn/giờ) và khóa/mở tài khoản.

#### Phase 3: Thanh toán Cọc PayOS/VNPay, Menu Đặt trước, Ưu đãi & Đánh giá (`Full Marketplace`)
- **`P3-T1` (K2):** API & UI Quản lý Thực đơn (`MENU_CATEGORIES`, `MENU_ITEMS`) và Chương trình Ưu đãi theo khung giờ (`PROMOTIONS`); hiển thị và chọn trên luồng đặt bàn.
- **`P3-T2` (K3):** Ghép logic Đặt món trước (`RESERVATION_ITEMS` snapshot giá), áp dụng Ưu đãi khung giờ, luồng Khách tự sửa/hủy đơn trước hạn chót và Cronjobs tự động (`EXPIRED` 15p & `NO_SHOW` 45p).
- **`P3-T3` (K5):** Tích hợp cổng thanh toán VietQR **PayOS** & **VNPay Sandbox** (tạo mã QR thanh toán cọc, xử lý Webhook bất biến, kịch bản thanh toán trễ sau khi đơn `EXPIRED`), Sổ cái đối soát nhà hàng (`RESTAURANT_LEDGER_ENTRIES`), Yêu cầu rút đối soát (`SETTLEMENT_PAYOUTS`) và Quản lý hoàn tiền cọc (`REFUNDS`).
- **`P3-T4` (K5 + K2):** API & UI Đánh giá xác thực sau bữa ăn (`REVIEWS` chỉ mở cho đơn `COMPLETED`, kèm ảnh Cloudinary, tự cập nhật `avg_rating` nhà hàng và phản hồi của Chủ quán) + Báo cáo thống kê cho Chủ quán & Admin sàn.

#### Phase 4: Kiểm thử Rào Lỗi (`E01–E32`) & Bàn giao Website Hoàn chỉnh
- **`P4-T1` (Cả nhóm, K5 & K1 điều phối):** Chạy kiểm thử nghiệm thu toàn bộ 32 kịch bản biên (`E01–E32`), kiểm thử phân quyền chéo nhà hàng, tối ưu UI Responsive trên Web, đóng gói Docker và deploy môi trường Demo trực tuyến.

#### Phase 5: Triển khai Ứng dụng Mobile Flutter (Đầy đủ Khách hàng & Quản lý Nhà hàng)
- **`P5-T1` (K1):** Dựng khung App Flutter (Clean Architecture / Feature-first, Dio JWT Refresh Interceptor, WebSocket client, Firebase FCM Push, phân tách Bottom Navigation tự động hoặc chuyển đổi chế độ **Khách hàng ↔ Quản lý Nhà hàng**).
- **`P5-T2` (K2):** Màn hình Flutter Khách hàng (Khám phá nhà hàng GPS gần tôi trên bản đồ, Bộ lọc, Trang chi tiết nhà hàng, Thực đơn & Ưu đãi) + Màn hình Flutter Chủ quán quản lý Hồ sơ, Ảnh, Thực đơn & Ưu đãi.
- **`P5-T3` (K3):** Màn hình Flutter Đặt bàn chọn slot & đặt món trước, Thanh toán QR PayOS/Deep link ngân hàng, Quản lý Đơn đặt bàn của tôi (hiển thị mã QR Check-in lớn, sửa/hủy đơn).
- **`P5-T4` (K4):** Màn hình Flutter Vận hành Nhà hàng: Sơ đồ bàn trực quan thời gian thực, **Camera Quét mã QR Check-in 1 chạm tại cửa**, Tạo nhanh Walk-in, Hàng chờ `WAITLIST`, Ghép/Đổi bàn, Cấu hình Ca mở cửa/Lịch Lễ & Nhân sự.
- **`P5-T5` (K5):** Màn hình Flutter Viết đánh giá & Phản hồi đánh giá, Ví đối soát nhà hàng & Yêu cầu rút tiền, Báo cáo biểu đồ vận hành nhà hàng trên di động; kiểm thử đồng bộ tức thời giữa App Flutter và Website React!

---

## 9. Tài liệu Kỹ thuật Tham chiếu
- [Spring Boot: Web & RESTful Services](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Spring Data JPA: Pessimistic Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [MySQL 8.4 Reference: InnoDB Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)
- [PayOS Developer Docs (VietQR Webhook)](https://payos.vn/docs/)
- [Flutter Networking & Camera QR Integration](https://docs.flutter.dev/data-and-backend/networking)
