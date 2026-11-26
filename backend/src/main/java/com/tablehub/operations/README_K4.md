# Module K4 — Restaurant Operations, Floor Plan, Schedule & Waitlist (Đợt 1 Độc Lập)

Package `com.tablehub.operations` chứa toàn bộ 6 bảng do K4 sở hữu (`DINING_TABLES`, `OPENING_HOURS`, `SPECIAL_SCHEDULES`, `RESTAURANT_MEMBERS`, `STAFF_INVITATIONS`, `WAITLIST_ENTRIES`), không chạm vào `pom.xml` hay file cấu hình chung của K1.

## Tích hợp khi K1 & K3 hoàn tất Phase 1
1. **K1** implement `RestaurantAccessPort` (gắn với SecurityContext & `RestaurantSecurityGuard`) và `NotificationMailPort` (gắn với `JavaMailSender`), đồng thời map `OperationsException` trong `@RestControllerAdvice` (`GlobalExceptionHandler`).
2. **K3** implement `ReservationImpactPort` (kiểm tra đơn `CHECKED_IN` / `CONFIRMED` tương lai cho quy tắc E05, E27).
3. **Đợt 2 (`P2-T4`):** Sau khi K3 hoàn tất `ReservationCommandService`, K4 bổ sung `LiveBoardController` nối các thao tác `WALK_IN`, `check-in-by-qr`, `extend`, `reassign-tables`, `complete`, `no-show`.
