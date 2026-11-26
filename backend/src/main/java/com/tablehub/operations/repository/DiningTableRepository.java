package com.tablehub.operations.repository;

import com.tablehub.operations.entity.DiningTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByRestaurantIdOrderByAreaNameAscTableCodeAsc(Long restaurantId);

    Optional<DiningTable> findByIdAndRestaurantId(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndTableCodeIgnoreCase(Long restaurantId, String tableCode);

    long countByRestaurantIdAndIsActiveTrue(Long restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DiningTable t WHERE t.id = :id AND t.restaurantId = :restaurantId")
    Optional<DiningTable> findByIdAndRestaurantIdForUpdate(
        @Param("id") Long id,
        @Param("restaurantId") Long restaurantId
    );
}
