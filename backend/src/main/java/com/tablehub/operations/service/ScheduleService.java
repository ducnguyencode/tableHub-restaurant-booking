package com.tablehub.operations.service;

import com.tablehub.operations.dto.ScheduleDtos.*;
import com.tablehub.operations.entity.OpeningHour;
import com.tablehub.operations.entity.SpecialSchedule;
import com.tablehub.operations.port.OperationsException;
import com.tablehub.operations.port.ReservationImpactPort;
import com.tablehub.operations.port.RestaurantAccessPort;
import com.tablehub.operations.repository.OpeningHourRepository;
import com.tablehub.operations.repository.SpecialScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ScheduleService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OpeningHourRepository openingHourRepository;
    private final SpecialScheduleRepository specialScheduleRepository;
    private final RestaurantAccessPort accessPort;
    private final ReservationImpactPort reservationImpactPort;

    public ScheduleService(
        OpeningHourRepository openingHourRepository,
        SpecialScheduleRepository specialScheduleRepository,
        RestaurantAccessPort accessPort,
        ReservationImpactPort reservationImpactPort
    ) {
        this.openingHourRepository = openingHourRepository;
        this.specialScheduleRepository = specialScheduleRepository;
        this.accessPort = accessPort;
        this.reservationImpactPort = reservationImpactPort;
    }

    @Transactional(readOnly = true)
    public List<OpeningHourResponse> listOpeningHours(Long restaurantId) {
        requireStaffOrOwner(restaurantId);
        return openingHourRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(restaurantId)
            .stream()
            .map(OpeningHourResponse::from)
            .toList();
    }

    @Transactional
    public OpeningHourResponse addOpeningHour(Long restaurantId, UpsertOpeningHourRequest req) {
        requireOwner(restaurantId);
        validateShiftTime(req);

        List<OpeningHour> existingShifts = openingHourRepository
            .findByRestaurantIdAndDayOfWeekOrderByOpensAtAsc(restaurantId, req.dayOfWeek());

        for (OpeningHour shift : existingShifts) {
            boolean overlaps = shift.getOpensAt().isBefore(req.closesAt())
                && shift.getClosesAt().isAfter(req.opensAt());
            if (overlaps) {
                throw new OperationsException(
                    HttpStatus.CONFLICT,
                    "OVERLAPPING_SHIFT",
                    "New shift (" + req.opensAt() + "-" + req.closesAt() + ") overlaps with existing shift " + shift.getShiftName()
                );
            }
        }

        OpeningHour entity = new OpeningHour();
        entity.setRestaurantId(restaurantId);
        entity.setDayOfWeek(req.dayOfWeek());
        entity.setShiftName(req.shiftName().trim());
        entity.setOpensAt(req.opensAt());
        entity.setClosesAt(req.closesAt());

        return OpeningHourResponse.from(openingHourRepository.save(entity));
    }

    @Transactional
    public void deleteOpeningHour(Long restaurantId, Long shiftId) {
        requireOwner(restaurantId);

        OpeningHour shift = openingHourRepository.findByIdAndRestaurantId(shiftId, restaurantId)
            .orElseThrow(() -> new OperationsException(HttpStatus.NOT_FOUND, "SHIFT_NOT_FOUND", "Opening shift not found"));

        if (accessPort.isRestaurantActive(restaurantId) && openingHourRepository.countByRestaurantId(restaurantId) <= 1) {
            throw new OperationsException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "LAST_OPENING_SHIFT",
                "An ACTIVE restaurant must retain at least one opening shift"
            );
        }

        long impacted = reservationImpactPort.countFutureConfirmedOutsideShift(
            restaurantId, shift.getDayOfWeek(), shift.getOpensAt(), shift.getClosesAt()
        );
        if (impacted > 0) {
            throw new OperationsException(
                HttpStatus.CONFLICT,
                "SHIFT_HAS_FUTURE_RESERVATIONS",
                "Cannot delete shift because " + impacted + " upcoming reservation(s) fall within this time range"
            );
        }

        openingHourRepository.delete(shift);
    }

    @Transactional(readOnly = true)
    public List<SpecialScheduleResponse> listUpcomingSpecialSchedules(Long restaurantId) {
        requireStaffOrOwner(restaurantId);
        LocalDate today = LocalDate.now(VN_ZONE);
        return specialScheduleRepository
            .findByRestaurantIdAndSpecificDateGreaterThanEqualOrderBySpecificDateAsc(restaurantId, today)
            .stream()
            .map(SpecialScheduleResponse::from)
            .toList();
    }

    @Transactional
    public SpecialScheduleResponse upsertSpecialSchedule(Long restaurantId, UpsertSpecialScheduleRequest req) {
        requireOwner(restaurantId);

        if (!req.isClosed()) {
            if (req.opensAt() == null || req.closesAt() == null || !req.opensAt().isBefore(req.closesAt())) {
                throw new OperationsException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SPECIAL_HOURS",
                    "When open on a special date, opensAt must be strictly before closesAt"
                );
            }
        } else {
            // Guardrail E05: Block sudden closure if future confirmed reservations exist on that date
            long impacted = reservationImpactPort.countFutureConfirmedOnDate(restaurantId, req.specificDate());
            if (impacted > 0) {
                throw new OperationsException(
                    HttpStatus.CONFLICT,
                    "DATE_HAS_FUTURE_RESERVATIONS",
                    "Date " + req.specificDate() + " has " + impacted + " confirmed reservation(s). Please cancel/refund them before closing."
                );
            }
        }

        SpecialSchedule entity = specialScheduleRepository
            .findByRestaurantIdAndSpecificDate(restaurantId, req.specificDate())
            .orElseGet(SpecialSchedule::new);

        entity.setRestaurantId(restaurantId);
        entity.setSpecificDate(req.specificDate());
        entity.setIsClosed(req.isClosed());
        entity.setOpensAt(req.isClosed() ? null : req.opensAt());
        entity.setClosesAt(req.isClosed() ? null : req.closesAt());
        entity.setForceDeposit(req.forceDeposit());
        entity.setDepositOverridePerGuest(req.depositOverridePerGuest());
        entity.setNote(req.note());

        return SpecialScheduleResponse.from(specialScheduleRepository.save(entity));
    }

    private void validateShiftTime(UpsertOpeningHourRequest req) {
        if (!req.opensAt().isBefore(req.closesAt())) {
            throw new OperationsException(
                HttpStatus.BAD_REQUEST,
                "INVALID_SHIFT_TIME",
                "opensAt must be strictly before closesAt within the same day"
            );
        }
    }

    private void requireOwner(Long restaurantId) {
        if (!accessPort.isUserOwner(restaurantId, accessPort.getCurrentUserId())) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only restaurant OWNER can perform this action");
        }
    }

    private void requireStaffOrOwner(Long restaurantId) {
        if (!accessPort.isUserStaffOrOwner(restaurantId, accessPort.getCurrentUserId())) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "User is not a member of this restaurant");
        }
    }
}
