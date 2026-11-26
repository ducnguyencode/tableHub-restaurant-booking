package com.tablehub.operations.port;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Port giao tiếp với module Đặt bàn (K3) để kiểm tra đơn tương lai bị ảnh hưởng (E05, E27)
 * trước khi cho phép sửa/tắt bàn hoặc thu hẹp/đóng cửa lịch hoạt động.
 */
public interface ReservationImpactPort {

    boolean hasActiveCheckedInOnTable(Long restaurantId, Long tableId);

    long countFutureConfirmedConflictingTable(Long restaurantId, Long tableId, int newCapacity, boolean willDisable);

    long countFutureConfirmedOutsideShift(Long restaurantId, int dayOfWeek, LocalTime newOpensAt, LocalTime newClosesAt);

    long countFutureConfirmedOnDate(Long restaurantId, LocalDate specificDate);
}
