package com.tablehub.operations.service;

import com.tablehub.operations.dto.WaitlistDtos.*;
import com.tablehub.operations.entity.WaitlistEntry;
import com.tablehub.operations.port.OperationsException;
import com.tablehub.operations.port.RestaurantAccessPort;
import com.tablehub.operations.repository.WaitlistEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class WaitlistService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WaitlistEntryRepository waitlistRepository;
    private final RestaurantAccessPort accessPort;

    public WaitlistService(WaitlistEntryRepository waitlistRepository, RestaurantAccessPort accessPort) {
        this.waitlistRepository = waitlistRepository;
        this.accessPort = accessPort;
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> listActiveQueue(Long restaurantId) {
        requireStaffOrOwner(restaurantId);
        return waitlistRepository
            .findByRestaurantIdAndStatusInOrderByCreatedAtAsc(
                restaurantId,
                List.of(WaitlistEntry.WaitlistStatus.WAITING, WaitlistEntry.WaitlistStatus.CALLED)
            )
            .stream()
            .map(WaitlistResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> suggestCandidatesForFreedCapacity(Long restaurantId, int freedCapacity) {
        requireStaffOrOwner(restaurantId);
        return waitlistRepository
            .findByRestaurantIdAndStatusAndGuestCountLessThanEqualOrderByCreatedAtAsc(
                restaurantId, WaitlistEntry.WaitlistStatus.WAITING, freedCapacity
            )
            .stream()
            .map(WaitlistResponse::from)
            .toList();
    }

    @Transactional
    public WaitlistResponse addToWaitlist(Long restaurantId, CreateWaitlistRequest req) {
        Long staffId = accessPort.getCurrentUserId();
        if (!accessPort.isUserStaffOrOwner(restaurantId, staffId)) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không thuộc nhân sự của nhà hàng này");
        }

        LocalDate today = LocalDate.now(VN_ZONE);
        OffsetDateTime startOfDay = today.atStartOfDay(VN_ZONE).toOffsetDateTime();
        OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay(VN_ZONE).toOffsetDateTime();
        long todayCount = waitlistRepository.countByRestaurantIdAndCreatedAtBetween(restaurantId, startOfDay, endOfDay);

        String queueNumber = String.format("Q%03d", todayCount + 1);

        WaitlistEntry entry = new WaitlistEntry();
        entry.setRestaurantId(restaurantId);
        entry.setQueueNumber(queueNumber);
        entry.setContactName(req.contactName().trim());
        entry.setContactPhone(req.contactPhone().trim());
        entry.setGuestCount(req.guestCount());
        entry.setStatus(WaitlistEntry.WaitlistStatus.WAITING);
        entry.setNote(req.note());
        entry.setCreatedBy(staffId);

        return WaitlistResponse.from(waitlistRepository.save(entry));
    }

    @Transactional
    public WaitlistResponse callGuest(Long restaurantId, Long entryId) {
        requireStaffOrOwner(restaurantId);
        WaitlistEntry entry = getEntryOrThrow(restaurantId, entryId);
        if (entry.getStatus() != WaitlistEntry.WaitlistStatus.WAITING) {
            throw new OperationsException(HttpStatus.CONFLICT, "INVALID_WAITLIST_STATUS", "Chỉ gọi được khách đang WAITING");
        }
        entry.setStatus(WaitlistEntry.WaitlistStatus.CALLED);
        entry.setCalledAt(OffsetDateTime.now(VN_ZONE));
        return WaitlistResponse.from(waitlistRepository.save(entry));
    }

    @Transactional
    public WaitlistResponse markSeated(Long restaurantId, Long entryId, SeatWaitlistRequest req) {
        requireStaffOrOwner(restaurantId);
        WaitlistEntry entry = getEntryOrThrow(restaurantId, entryId);
        if (entry.getStatus() != WaitlistEntry.WaitlistStatus.WAITING
            && entry.getStatus() != WaitlistEntry.WaitlistStatus.CALLED) {
            throw new OperationsException(HttpStatus.CONFLICT, "INVALID_WAITLIST_STATUS", "Phiếu chờ không còn hiệu lực xếp bàn");
        }
        entry.setStatus(WaitlistEntry.WaitlistStatus.SEATED);
        entry.setSeatedReservationId(req.seatedReservationId());
        entry.setSeatedAt(OffsetDateTime.now(VN_ZONE));
        return WaitlistResponse.from(waitlistRepository.save(entry));
    }

    @Transactional
    public WaitlistResponse skipGuest(Long restaurantId, Long entryId) {
        requireStaffOrOwner(restaurantId);
        WaitlistEntry entry = getEntryOrThrow(restaurantId, entryId);
        if (entry.getStatus() != WaitlistEntry.WaitlistStatus.WAITING
            && entry.getStatus() != WaitlistEntry.WaitlistStatus.CALLED) {
            throw new OperationsException(HttpStatus.CONFLICT, "INVALID_WAITLIST_STATUS", "Không thể bỏ qua phiếu ở trạng thái hiện tại");
        }
        entry.setStatus(WaitlistEntry.WaitlistStatus.SKIPPED);
        return WaitlistResponse.from(waitlistRepository.save(entry));
    }

    private WaitlistEntry getEntryOrThrow(Long restaurantId, Long entryId) {
        return waitlistRepository.findByIdAndRestaurantId(entryId, restaurantId)
            .orElseThrow(() -> new OperationsException(HttpStatus.NOT_FOUND, "WAITLIST_NOT_FOUND", "Không tìm thấy phiếu hàng chờ"));
    }

    private void requireStaffOrOwner(Long restaurantId) {
        if (!accessPort.isUserStaffOrOwner(restaurantId, accessPort.getCurrentUserId())) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không thuộc nhân sự của nhà hàng này");
        }
    }
}
