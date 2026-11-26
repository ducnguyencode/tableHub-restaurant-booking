package com.tablehub.operations.controller;

import com.tablehub.operations.dto.ScheduleDtos.*;
import com.tablehub.operations.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/management/restaurants/{restaurantId}")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/opening-hours")
    public ResponseEntity<List<OpeningHourResponse>> listOpeningHours(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(scheduleService.listOpeningHours(restaurantId));
    }

    @PostMapping("/opening-hours")
    public ResponseEntity<OpeningHourResponse> addOpeningHour(
        @PathVariable Long restaurantId,
        @Valid @RequestBody UpsertOpeningHourRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.addOpeningHour(restaurantId, request));
    }

    @DeleteMapping("/opening-hours/{shiftId}")
    public ResponseEntity<Void> deleteOpeningHour(
        @PathVariable Long restaurantId,
        @PathVariable Long shiftId
    ) {
        scheduleService.deleteOpeningHour(restaurantId, shiftId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/special-schedules")
    public ResponseEntity<List<SpecialScheduleResponse>> listUpcomingSpecialSchedules(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(scheduleService.listUpcomingSpecialSchedules(restaurantId));
    }

    @PutMapping("/special-schedules")
    public ResponseEntity<SpecialScheduleResponse> upsertSpecialSchedule(
        @PathVariable Long restaurantId,
        @Valid @RequestBody UpsertSpecialScheduleRequest request
    ) {
        return ResponseEntity.ok(scheduleService.upsertSpecialSchedule(restaurantId, request));
    }
}
