package com.inventory.controller;

import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.DashboardStatsResponse;
import com.inventory.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }
}
