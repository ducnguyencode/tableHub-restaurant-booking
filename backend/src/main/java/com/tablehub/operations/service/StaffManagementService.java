package com.tablehub.operations.service;

import com.tablehub.operations.dto.StaffDtos.*;
import com.tablehub.operations.entity.RestaurantMember;
import com.tablehub.operations.entity.StaffInvitation;
import com.tablehub.operations.port.NotificationMailPort;
import com.tablehub.operations.port.OperationsException;
import com.tablehub.operations.port.RestaurantAccessPort;
import com.tablehub.operations.repository.RestaurantMemberRepository;
import com.tablehub.operations.repository.StaffInvitationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class StaffManagementService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RestaurantMemberRepository memberRepository;
    private final StaffInvitationRepository invitationRepository;
    private final RestaurantAccessPort accessPort;
    private final NotificationMailPort mailPort;

    public StaffManagementService(
        RestaurantMemberRepository memberRepository,
        StaffInvitationRepository invitationRepository,
        RestaurantAccessPort accessPort,
        NotificationMailPort mailPort
    ) {
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.accessPort = accessPort;
        this.mailPort = mailPort;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long restaurantId) {
        requireOwner(restaurantId);
        return memberRepository.findByRestaurantIdOrderByMemberRoleAscCreatedAtAsc(restaurantId)
            .stream()
            .map(MemberResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(Long restaurantId) {
        requireOwner(restaurantId);
        return invitationRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
            .stream()
            .map(InvitationResponse::from)
            .toList();
    }

    @Transactional
    public InvitationResponse inviteStaff(Long restaurantId, InviteStaffRequest req) {
        Long inviterId = accessPort.getCurrentUserId();
        if (!accessPort.isUserOwner(restaurantId, inviterId)) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ OWNER mới được mời nhân viên");
        }

        String normalizedEmail = req.email().trim().toLowerCase();
        boolean hasPending = invitationRepository.existsByRestaurantIdAndEmailIgnoreCaseAndStatus(
            restaurantId, normalizedEmail, StaffInvitation.InvitationStatus.PENDING
        );
        if (hasPending) {
            throw new OperationsException(
                HttpStatus.CONFLICT,
                "INVITATION_ALREADY_PENDING",
                "Email " + normalizedEmail + " đang có lời mời chờ xác nhận"
            );
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        StaffInvitation invitation = new StaffInvitation();
        invitation.setRestaurantId(restaurantId);
        invitation.setEmail(normalizedEmail);
        invitation.setInvitedRole(req.invitedRole());
        invitation.setTokenHash(sha256(rawToken));
        invitation.setInvitedBy(inviterId);
        invitation.setStatus(StaffInvitation.InvitationStatus.PENDING);
        invitation.setExpiresAt(OffsetDateTime.now(VN_ZONE).plusHours(48));

        StaffInvitation saved = invitationRepository.save(invitation);
        mailPort.sendStaffInvitationEmail(normalizedEmail, restaurantId, req.invitedRole().name(), rawToken);
        return InvitationResponse.from(saved);
    }

    @Transactional
    public MemberResponse acceptInvitation(String rawToken) {
        Long currentUserId = accessPort.getCurrentUserId();
        StaffInvitation invitation = invitationRepository.findByTokenHash(sha256(rawToken))
            .orElseThrow(() -> new OperationsException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "Lời mời không tồn tại"));

        if (invitation.getStatus() != StaffInvitation.InvitationStatus.PENDING) {
            throw new OperationsException(HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "Lời mời đã được sử dụng hoặc thu hồi");
        }

        if (OffsetDateTime.now(VN_ZONE).isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(StaffInvitation.InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new OperationsException(HttpStatus.GONE, "INVITATION_EXPIRED", "Lời mời đã hết hạn (48 giờ)");
        }

        RestaurantMember member = memberRepository
            .findByRestaurantIdAndUserId(invitation.getRestaurantId(), currentUserId)
            .orElseGet(RestaurantMember::new);

        member.setRestaurantId(invitation.getRestaurantId());
        member.setUserId(currentUserId);
        member.setMemberRole(invitation.getInvitedRole());
        member.setStatus(RestaurantMember.MemberStatus.ACTIVE);

        RestaurantMember savedMember = memberRepository.save(member);
        invitation.setStatus(StaffInvitation.InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return MemberResponse.from(savedMember);
    }

    @Transactional
    public MemberResponse updateMember(Long restaurantId, Long memberId, UpdateMemberRequest req) {
        requireOwner(restaurantId);

        // Khóa danh sách thành viên active để rào E28 (bảo vệ OWNER cuối cùng)
        List<RestaurantMember> activeMembers = memberRepository.findActiveMembersForUpdate(restaurantId);
        RestaurantMember target = memberRepository.findByIdAndRestaurantId(memberId, restaurantId)
            .orElseThrow(() -> new OperationsException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy nhân sự"));

        boolean targetWasActiveOwner = target.getStatus() == RestaurantMember.MemberStatus.ACTIVE
            && target.getMemberRole() == RestaurantMember.MemberRole.OWNER;
        boolean targetRemainsActiveOwner = req.status() == RestaurantMember.MemberStatus.ACTIVE
            && req.memberRole() == RestaurantMember.MemberRole.OWNER;

        if (targetWasActiveOwner && !targetRemainsActiveOwner) {
            long activeOwnersCount = activeMembers.stream()
                .filter(m -> m.getMemberRole() == RestaurantMember.MemberRole.OWNER)
                .count();
            if (activeOwnersCount <= 1) {
                throw new OperationsException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "LAST_ACTIVE_OWNER",
                    "Không thể xóa hoặc hạ quyền OWNER cuối cùng của nhà hàng"
                );
            }
        }

        target.setMemberRole(req.memberRole());
        target.setStatus(req.status());
        return MemberResponse.from(memberRepository.save(target));
    }

    private void requireOwner(Long restaurantId) {
        if (!accessPort.isUserOwner(restaurantId, accessPort.getCurrentUserId())) {
            throw new OperationsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ OWNER nhà hàng mới có quyền quản lý nhân sự");
        }
    }

    private static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
