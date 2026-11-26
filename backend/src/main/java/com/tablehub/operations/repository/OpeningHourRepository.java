package com.tablehub.operations.repository;

import com.tablehub.operations.entity.OpeningHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OpeningHourRepository extends JpaRepository<OpeningHour, Long> {

    List<OpeningHour> findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(Long restaurantId);

    List<OpeningHour> findByRestaurantIdAndDayOfWeekOrderByOpensAtAsc(Long restaurantId, Integer dayOfWeek);

    Optional<OpeningHour> findByIdAndRestaurantId(Long id, Long restaurantId);

    long countByRestaurantId(Long restaurantId);
}
