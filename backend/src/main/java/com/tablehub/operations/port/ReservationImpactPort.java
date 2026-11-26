package com.tablehub.operations.port;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Port for communicating with the Booking module (K3) to check impacted future
 * reservations (rules E05, E27) before modifying tables or schedules.
 */
public interface ReservationImpactPort {

    boolean hasActiveCheckedInOnTable(Long restaurantId, Long tableId);

    long countFutureConfirmedConflictingTable(Long restaurantId, Long tableId, int newCapacity, boolean willDisable);

    long countFutureConfirmedOutsideShift(Long restaurantId, int dayOfWeek, LocalTime newOpensAt, LocalTime newClosesAt);

    long countFutureConfirmedOnDate(Long restaurantId, LocalDate specificDate);
}
