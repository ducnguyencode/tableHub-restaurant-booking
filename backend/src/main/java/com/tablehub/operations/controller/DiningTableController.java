package com.tablehub.operations.controller;

import com.tablehub.operations.dto.TableDtos.*;
import com.tablehub.operations.service.DiningTableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/management/restaurants/{restaurantId}/tables")
public class DiningTableController {

    private final DiningTableService tableService;

    public DiningTableController(DiningTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> listTables(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(tableService.listTables(restaurantId));
    }

    @PostMapping
    public ResponseEntity<TableResponse> createTable(
        @PathVariable Long restaurantId,
        @Valid @RequestBody CreateTableRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(restaurantId, request));
    }

    @PutMapping("/{tableId}")
    public ResponseEntity<TableResponse> updateTable(
        @PathVariable Long restaurantId,
        @PathVariable Long tableId,
        @Valid @RequestBody UpdateTableRequest request
    ) {
        return ResponseEntity.ok(tableService.updateTable(restaurantId, tableId, request));
    }
}
