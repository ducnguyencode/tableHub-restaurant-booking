package com.tablehub.operations.controller;

import com.tablehub.operations.dto.StaffDtos.*;
import com.tablehub.operations.service.StaffManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StaffManagementController {

    private final StaffManagementService staffService;

    public StaffManagementController(StaffManagementService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/management/restaurants/{restaurantId}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(staffService.listMembers(restaurantId));
    }

    @PutMapping("/management/restaurants/{restaurantId}/members/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
        @PathVariable Long restaurantId,
        @PathVariable Long memberId,
        @Valid @RequestBody UpdateMemberRequest request
    ) {
        return ResponseEntity.ok(staffService.updateMember(restaurantId, memberId, request));
    }

    @GetMapping("/management/restaurants/{restaurantId}/invitations")
    public ResponseEntity<List<InvitationResponse>> listInvitations(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(staffService.listInvitations(restaurantId));
    }

    @PostMapping("/management/restaurants/{restaurantId}/invitations")
    public ResponseEntity<InvitationResponse> inviteStaff(
        @PathVariable Long restaurantId,
        @Valid @RequestBody InviteStaffRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.inviteStaff(restaurantId, request));
    }

    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<MemberResponse> acceptInvitation(@PathVariable String token) {
        return ResponseEntity.ok(staffService.acceptInvitation(token));
    }
}
