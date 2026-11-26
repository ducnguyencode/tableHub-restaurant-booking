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
        @NotBlank(message = "Tên khách hàng chờ bàn là bắt buộc")
        @Size(max = 120)
        String contactName,

        @NotBlank(message = "Số điện thoại khách chờ bàn là bắt buộc")
        @Size(max = 30)
        String contactPhone,

        @NotNull
        @Min(value = 1, message = "Số khách phải >= 1")
        Integer guestCount,

        @Size(max = 255)
        String note
    ) {}

    public record SeatWaitlistRequest(
        @NotNull(message = "Cần ID lượt đặt bàn WALK_IN vừa tạo để liên kết")
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
