package com.tablehub.operations.dto;

import com.tablehub.operations.entity.WaitlistEntry;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class WaitlistDtos {
    private WaitlistDtos() {}

    public record CreateWaitlistRequest(
        @NotBlank(message = "Contact name is required for waitlist")
        @Size(max = 120)
        String contactName,

        @NotBlank(message = "Contact phone is required for waitlist")
        @Size(max = 30)
        String contactPhone,

        @NotNull
        @Min(value = 1, message = "Guest count must be >= 1")
        Integer guestCount,

        @Size(max = 255)
        String note
    ) {}

    public record SeatWaitlistRequest(
        @NotNull(message = "Seated WALK_IN reservation ID is required")
        Long seatedReservationId
    ) {}

    public record WaitlistResponse(
        Long id,
        Long restaurantId,
        String queueNumber,
        String contactName,
        String contactPhone,
        Integer guestCount,
        WaitlistEntry.WaitlistStatus status,
        Long seatedReservationId,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime calledAt,
        OffsetDateTime seatedAt
    ) {
        public static WaitlistResponse from(WaitlistEntry w) {
            return new WaitlistResponse(
                w.getId(), w.getRestaurantId(), w.getQueueNumber(),
                w.getContactName(), w.getContactPhone(), w.getGuestCount(),
                w.getStatus(), w.getSeatedReservationId(), w.getNote(),
                w.getCreatedAt(), w.getCalledAt(), w.getSeatedAt()
            );
        }
    }
}
