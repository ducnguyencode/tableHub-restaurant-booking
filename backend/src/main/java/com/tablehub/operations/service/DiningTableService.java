package com.tablehub.operations.service;

import com.tablehub.operations.dto.TableDtos.*;
import com.tablehub.operations.entity.DiningTable;
import com.tablehub.operations.port.OperationsException;
import com.tablehub.operations.port.ReservationImpactPort;
import com.tablehub.operations.port.RestaurantAccessPort;
import com.tablehub.operations.repository.DiningTableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiningTableService {

    private final DiningTableRepository tableRepository;
    private final RestaurantAccessPort accessPort;
    private final ReservationImpactPort reservationImpactPort;

    public DiningTableService(
        DiningTableRepository tableRepository,
        RestaurantAccessPort accessPort,
        ReservationImpactPort reservationImpactPort
    ) {
        this.tableRepository = tableRepository;
        this.accessPort = accessPort;
        this.reservationImpactPort = reservationImpactPort;
    }

    @Transactional(readOnly = true)
    public List<TableResponse> listTables(Long restaurantId) {
        requireStaffOrOwner(restaurantId);
        return tableRepository.findByRestaurantIdOrderByAreaNameAscTableCodeAsc(restaurantId)
            .stream()
            .map(TableResponse::from)
            .toList();
    }

    @Transactional
    public TableResponse createTable(Long restaurantId, CreateTableRequest req) {
        requireOwner(restaurantId);

        int minParty = req.minPartySize() != null ? req.minPartySize() : 1;
        if (minParty > req.capacity()) {
            throw new OperationsException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARTY_RANGE",
                "Số khách tối thiểu (minPartySize) không được lớn hơn sức chứa bàn"
            );
        }

        String normalizedCode = req.tableCode().trim().toUpperCase();
        if (tableRepository.existsByRestaurantIdAndTableCodeIgnoreCase(restaurantId, normalizedCode)) {
            throw new OperationsException(
                HttpStatus.CONFLICT,
                "DUPLICATE_TABLE_CODE",
                "Mã bàn " + normalizedCode + " đã tồn tại trong nhà hàng"
            );
        }

        DiningTable table = new DiningTable();
        table.setRestaurantId(restaurantId);
        table.setAreaName(req.areaName().trim());
        table.setTableCode(normalizedCode);
        table.setMinPartySize(minParty);
        table.setCapacity(req.capacity());
        table.setIsCombinable(Boolean.TRUE.equals(req.isCombinable()));
        table.setPosX(req.posX());
        table.setPosY(req.posY());
        table.setIsActive(true);

        return TableResponse.from(tableRepository.save(table));
    }

    @Transactional
    public TableResponse updateTable(Long restaurantId, Long tableId, UpdateTableRequest req) {
        requireOwner(restaurantId);

        if (req.minPartySize() > req.capacity()) {
            throw new OperationsException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARTY_RANGE",
                "Số khách tối thiểu không được lớn hơn sức chứa bàn"
            );
        }

        DiningTable table = tableRepository.findByIdAndRestaurantIdForUpdate(tableId, restaurantId)
            .orElseThrow(() -> new OperationsException(HttpStatus.NOT_FOUND, "TABLE_NOT_FOUND", "Không tìm thấy bàn"));

        String normalizedCode = req.tableCode().trim().toUpperCase();
        if (!table.getTableCode().equalsIgnoreCase(normalizedCode)
            && tableRepository.existsByRestaurantIdAndTableCodeIgnoreCase(restaurantId, normalizedCode)) {
            throw new OperationsException(
                HttpStatus.CONFLICT,
                "DUPLICATE_TABLE_CODE",
                "Mã bàn " + normalizedCode + " đã tồn tại trong nhà hàng"
            );
        }

        boolean willDisable = Boolean.TRUE.equals(table.getIsActive()) && Boolean.FALSE.equals(req.isActive());
        boolean capacityReduced = req.capacity() < table.getCapacity();

        // Rào E27: Không cho tắt bàn hoặc giảm sức chứa nếu đang có khách ngồi hoặc vướng đơn tương lai
        if (willDisable || capacityReduced) {
            if (reservationImpactPort.hasActiveCheckedInOnTable(restaurantId, tableId)) {
                throw new OperationsException(
                    HttpStatus.CONFLICT,
                    "TABLE_CURRENTLY_OCCUPIED",
                    "Bàn đang có khách CHECKED_IN, không thể tắt bàn hoặc giảm sức chứa lúc này"
                );
            }
            long conflicting = reservationImpactPort.countFutureConfirmedConflictingTable(
                restaurantId, tableId, req.capacity(), willDisable
            );
            if (conflicting > 0) {
                throw new OperationsException(
                    HttpStatus.CONFLICT,
                    "TABLE_HAS_FUTURE_RESERVATIONS",
                    "Có " + conflicting + " đơn đặt bàn tương lai bị ảnh hưởng. Vui lòng đổi bàn cho các đơn này trước khi lưu."
                );
            }
        }

        // Rào E20: Không cho tắt bàn active cuối cùng khi nhà hàng đang ACTIVE
        if (willDisable && accessPort.isRestaurantActive(restaurantId)) {
            long activeCount = tableRepository.countByRestaurantIdAndIsActiveTrue(restaurantId);
            if (activeCount <= 1) {
                throw new OperationsException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "LAST_ACTIVE_TABLE",
                    "Nhà hàng đang ACTIVE phải có ít nhất 1 bàn hoạt động"
                );
            }
        }

        table.setAreaName(req.areaName().trim());
        table.setTableCode(normalizedCode);
        table.setMinPartySize(req.minPartySize());
        table.setCapacity(req.capacity());
        table.setIsCombinable(req.isCombinable());
        table.setPosX(req.posX());
        table.setPosY(req.posY());
        table.setIsActive(req.isActive());

        return TableResponse.from(tableRepository.save(table));
    }

    private void requireOwner(Long restaurantId) {
        Long userId = accessPort.getCurrentUserId();
        if (!accessPort.isUserOwner(restaurantId, userId)) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ OWNER nhà hàng mới có quyền thực hiện");
        }
    }

    private void requireStaffOrOwner(Long restaurantId) {
        Long userId = accessPort.getCurrentUserId();
        if (!accessPort.isUserStaffOrOwner(restaurantId, userId)) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không thuộc nhân sự của nhà hàng này");
        }
    }
}
