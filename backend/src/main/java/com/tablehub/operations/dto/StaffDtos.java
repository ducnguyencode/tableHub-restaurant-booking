package com.tablehub.operations.dto;

import com.tablehub.operations.entity.RestaurantMember;
import com.tablehub.operations.entity.StaffInvitation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public final class StaffDtos {
    private StaffDtos() {}

    public record InviteStaffRequest(
        @NotBlank(message = "Staff email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Invited role is required (OWNER or STAFF)")
        RestaurantMember.MemberRole invitedRole
    ) {}

    public record UpdateMemberRequest(
        @NotNull RestaurantMember.MemberRole memberRole,
        @NotNull RestaurantMember.MemberStatus status
    ) {}

    public record MemberResponse(
        Long id,
        Long restaurantId,
        Long userId,
        RestaurantMember.MemberRole memberRole,
        RestaurantMember.MemberStatus status,
        OffsetDateTime createdAt
    ) {
        public static MemberResponse from(RestaurantMember m) {
            return new MemberResponse(
                m.getId(), m.getRestaurantId(), m.getUserId(),
                m.getMemberRole(), m.getStatus(), m.getCreatedAt()
            );
        }
    }

    public record InvitationResponse(
        Long id,
        Long restaurantId,
        String email,
        RestaurantMember.MemberRole invitedRole,
        StaffInvitation.InvitationStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
    ) {
        public static InvitationResponse from(StaffInvitation i) {
            return new InvitationResponse(
                i.getId(), i.getRestaurantId(), i.getEmail(),
                i.getInvitedRole(), i.getStatus(), i.getExpiresAt(), i.getCreatedAt()
            );
        }
    }
}
