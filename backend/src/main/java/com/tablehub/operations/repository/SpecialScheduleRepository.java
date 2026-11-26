package com.tablehub.operations.repository;

import com.tablehub.operations.entity.SpecialSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpecialScheduleRepository extends JpaRepository<SpecialSchedule, Long> {

    List<SpecialSchedule> findByRestaurantIdAndSpecificDateGreaterThanEqualOrderBySpecificDateAsc(
        Long restaurantId,
        LocalDate fromDate
    );

    Optional<SpecialSchedule> findByRestaurantIdAndSpecificDate(Long restaurantId, LocalDate specificDate);

    Optional<SpecialSchedule> findByIdAndRestaurantId(Long id, Long restaurantId);
}
