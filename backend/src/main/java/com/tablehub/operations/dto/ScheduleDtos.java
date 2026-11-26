package com.tablehub.operations.dto;

import com.tablehub.operations.entity.OpeningHour;
import com.tablehub.operations.entity.SpecialSchedule;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    public record UpsertOpeningHourRequest(
        @NotNull
        @Min(value = 1, message = "dayOfWeek must be between 1 (Monday) and 7 (Sunday)")
        @Max(value = 7, message = "dayOfWeek must be between 1 (Monday) and 7 (Sunday)")
        Integer dayOfWeek,

        @NotBlank(message = "Shift name is required (e.g., Lunch Shift, Dinner Shift)")
        @Size(max = 60)
        String shiftName,

        @NotNull(message = "Opening time is required")
        LocalTime opensAt,

        @NotNull(message = "Closing time is required")
        LocalTime closesAt
    ) {}

    public record OpeningHourResponse(
        Long id,
        Long restaurantId,
        Integer dayOfWeek,
        String shiftName,
        LocalTime opensAt,
        LocalTime closesAt
    ) {
        public static OpeningHourResponse from(OpeningHour o) {
            return new OpeningHourResponse(
                o.getId(), o.getRestaurantId(), o.getDayOfWeek(),
                o.getShiftName(), o.getOpensAt(), o.getClosesAt()
            );
        }
    }

    public record UpsertSpecialScheduleRequest(
        @NotNull(message = "Specific date is required")
        LocalDate specificDate,

        @NotNull Boolean isClosed,
        LocalTime opensAt,
        LocalTime closesAt,
        @NotNull Boolean forceDeposit,
        @Min(0) Long depositOverridePerGuest,
        @Size(max = 255) String note
    ) {}

    public record SpecialScheduleResponse(
        Long id,
        Long restaurantId,
        LocalDate specificDate,
        Boolean isClosed,
        LocalTime opensAt,
        LocalTime closesAt,
        Boolean forceDeposit,
        Long depositOverridePerGuest,
        String note
    ) {
        public static SpecialScheduleResponse from(SpecialSchedule s) {
            return new SpecialScheduleResponse(
                s.getId(), s.getRestaurantId(), s.getSpecificDate(),
                s.getIsClosed(), s.getOpensAt(), s.getClosesAt(),
                s.getForceDeposit(), s.getDepositOverridePerGuest(), s.getNote()
            );
        }
    }
}
