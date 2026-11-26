package com.tablehub.operations.controller;

import com.tablehub.operations.dto.WaitlistDtos.*;
import com.tablehub.operations.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/management/restaurants/{restaurantId}/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @GetMapping
    public ResponseEntity<List<WaitlistResponse>> listActiveQueue(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(waitlistService.listActiveQueue(restaurantId));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<WaitlistResponse>> suggestCandidates(
        @PathVariable Long restaurantId,
        @RequestParam int freedCapacity
    ) {
        return ResponseEntity.ok(waitlistService.suggestCandidatesForFreedCapacity(restaurantId, freedCapacity));
    }

    @PostMapping
    public ResponseEntity<WaitlistResponse> addToWaitlist(
        @PathVariable Long restaurantId,
        @Valid @RequestBody CreateWaitlistRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.addToWaitlist(restaurantId, request));
    }

    @PostMapping("/{entryId}/call")
    public ResponseEntity<WaitlistResponse> callGuest(
        @PathVariable Long restaurantId,
        @PathVariable Long entryId
    ) {
        return ResponseEntity.ok(waitlistService.callGuest(restaurantId, entryId));
    }

    @PostMapping("/{entryId}/seat")
    public ResponseEntity<WaitlistResponse> markSeated(
        @PathVariable Long restaurantId,
        @PathVariable Long entryId,
        @Valid @RequestBody SeatWaitlistRequest request
    ) {
        return ResponseEntity.ok(waitlistService.markSeated(restaurantId, entryId, request));
    }

    @PostMapping("/{entryId}/skip")
    public ResponseEntity<WaitlistResponse> skipGuest(
        @PathVariable Long restaurantId,
        @PathVariable Long entryId
    ) {
        return ResponseEntity.ok(waitlistService.skipGuest(restaurantId, entryId));
    }
}
