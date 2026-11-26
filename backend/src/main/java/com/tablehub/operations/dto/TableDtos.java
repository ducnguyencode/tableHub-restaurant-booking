package com.tablehub.operations.dto;

import com.tablehub.operations.entity.DiningTable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TableDtos {
    private TableDtos() {}

    public record CreateTableRequest(
        @NotBlank(message = "Area name is required")
        @Size(max = 100)
        String areaName,

        @NotBlank(message = "Table code is required")
        @Size(max = 30)
        String tableCode,

        @Min(value = 1, message = "Minimum party size must be >= 1")
        Integer minPartySize,

        @NotNull(message = "Table capacity is required")
        @Min(value = 1, message = "Table capacity must be >= 1")
        Integer capacity,

        Boolean isCombinable,
        Integer posX,
        Integer posY
    ) {}

    public record UpdateTableRequest(
        @NotBlank(message = "Area name is required")
        @Size(max = 100)
        String areaName,

        @NotBlank(message = "Table code is required")
        @Size(max = 30)
        String tableCode,

        @NotNull
        @Min(value = 1, message = "Minimum party size must be >= 1")
        Integer minPartySize,

        @NotNull
        @Min(value = 1, message = "Table capacity must be >= 1")
        Integer capacity,

        @NotNull Boolean isCombinable,
        Integer posX,
        Integer posY,
        @NotNull Boolean isActive
    ) {}

    public record TableResponse(
        Long id,
        Long restaurantId,
        String areaName,
        String tableCode,
        Integer minPartySize,
        Integer capacity,
        Boolean isCombinable,
        Integer posX,
        Integer posY,
        Boolean isActive
    ) {
        public static TableResponse from(DiningTable t) {
            return new TableResponse(
                t.getId(), t.getRestaurantId(), t.getAreaName(), t.getTableCode(),
                t.getMinPartySize(), t.getCapacity(), t.getIsCombinable(),
                t.getPosX(), t.getPosY(), t.getIsActive()
            );
        }
    }
}
