# K4 Module — Restaurant Operations, Floor Plan, Schedule & Waitlist (Batch 1)

Package `com.tablehub.operations` owns the 6 operational tables (`DINING_TABLES`, `OPENING_HOURS`, `SPECIAL_SCHEDULES`, `RESTAURANT_MEMBERS`, `STAFF_INVITATIONS`, `WAITLIST_ENTRIES`), isolated from `pom.xml` and shared configurations owned by K1.

## Integration Checklist (After K1 & K3 Complete Phase 1)
1. **K1** implements `RestaurantAccessPort` (wired to Spring Security & `RestaurantSecurityGuard`) and `NotificationMailPort` (wired to `JavaMailSender`), and maps `OperationsException` inside `@RestControllerAdvice` (`GlobalExceptionHandler`).
2. **K3** implements `ReservationImpactPort` (checking active `CHECKED_IN` and upcoming `CONFIRMED` reservations for rules E05 and E27).
3. **Batch 2 (`P2-T4`):** Once K3 completes `ReservationCommandService`, K4 adds `LiveBoardController` for `WALK_IN`, `check-in-by-qr`, `extend`, `reassign-tables`, `complete`, and `no-show`.
