package com.tablehub.operations.repository;

import com.tablehub.operations.entity.RestaurantMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantMemberRepository extends JpaRepository<RestaurantMember, Long> {

    List<RestaurantMember> findByRestaurantIdOrderByMemberRoleAscCreatedAtAsc(Long restaurantId);

    Optional<RestaurantMember> findByRestaurantIdAndUserId(Long restaurantId, Long userId);

    Optional<RestaurantMember> findByIdAndRestaurantId(Long id, Long restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM RestaurantMember m WHERE m.restaurantId = :restaurantId AND m.status = 'ACTIVE'")
    List<RestaurantMember> findActiveMembersForUpdate(@Param("restaurantId") Long restaurantId);
}
