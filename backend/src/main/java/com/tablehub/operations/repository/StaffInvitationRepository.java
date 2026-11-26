package com.tablehub.operations.repository;

import com.tablehub.operations.entity.StaffInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {

    List<StaffInvitation> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    Optional<StaffInvitation> findByTokenHash(String tokenHash);

    Optional<StaffInvitation> findByIdAndRestaurantId(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndEmailIgnoreCaseAndStatus(
        Long restaurantId,
        String email,
        StaffInvitation.InvitationStatus status
    );
}
