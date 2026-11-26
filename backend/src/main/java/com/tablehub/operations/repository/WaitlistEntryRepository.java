package com.tablehub.operations.repository;

import com.tablehub.operations.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByRestaurantIdAndStatusInOrderByCreatedAtAsc(
        Long restaurantId,
        List<WaitlistEntry.WaitlistStatus> statuses
    );

    Optional<WaitlistEntry> findByIdAndRestaurantId(Long id, Long restaurantId);

    long countByRestaurantIdAndCreatedAtBetween(
        Long restaurantId,
        OffsetDateTime startOfDay,
        OffsetDateTime endOfDay
    );

    List<WaitlistEntry> findByRestaurantIdAndStatusAndGuestCountLessThanEqualOrderByCreatedAtAsc(
        Long restaurantId,
        WaitlistEntry.WaitlistStatus status,
        Integer maxCapacity
    );
}
