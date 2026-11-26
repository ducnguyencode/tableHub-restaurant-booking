# TableHub — Portal Đặt Bàn Online & Điều Phối Đa Nhà Hàng

Repository gốc cho đồ án HK4 (Web, Backend API và Mobile App).

## Cấu trúc Monorepo

- `backend/` — Java Spring Boot 3 + MySQL 8 + Flyway (REST API & WebSocket dùng chung)
- `frontend/` — Web Portal (React + Vite)
- `mobile/` — Mobile App (Flutter — giai đoạn sau)
- `docs/` — Tài liệu kế hoạch triển khai và sơ đồ ERD (`ke-hoach-trien-khai.md`, `erd.mmd`)

## Quy ước Git cho nhóm (5 thành viên K1–K5)

- `main`: Nhánh ổn định dùng để demo/deploy.
- `develop`: Nhánh mặc định để tích hợp code hàng ngày.
- Mỗi task tạo nhánh riêng từ `develop` (ví dụ: `feat/k1-auth-jwt`, `feat/k3-booking-lock`), sau đó tạo Pull Request về `develop` để thành viên khác review trước khi merge.
