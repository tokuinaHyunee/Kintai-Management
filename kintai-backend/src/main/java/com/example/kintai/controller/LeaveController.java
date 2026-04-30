package com.example.kintai.controller;

import com.example.kintai.dto.CreateLeaveRequestDto;
import com.example.kintai.dto.LeaveRequestDto;
import com.example.kintai.dto.RejectLeaveRequestDto;
import com.example.kintai.service.LeaveService;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeResolver employeeResolver;

    @PostMapping("/api/leaves")
    public ResponseEntity<?> applyLeave(@RequestBody CreateLeaveRequestDto req) {
        leaveService.apply(employeeResolver.resolve(), req);
        return ResponseEntity.ok(Map.of("message", "休暇申請を送信しました"));
    }

    @GetMapping("/api/leaves")
    public ResponseEntity<List<LeaveRequestDto>> getMyLeaves() {
        return ResponseEntity.ok(leaveService.getMyLeaves(employeeResolver.resolve()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/leaves")
    public ResponseEntity<List<LeaveRequestDto>> getAllLeaves(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(leaveService.getAllLeaves(status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/leaves/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", leaveService.getPendingCount()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/leaves/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        leaveService.approve(id);
        return ResponseEntity.ok(Map.of("message", "承認しました"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/leaves/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody RejectLeaveRequestDto req) {
        leaveService.reject(id, req.getRejectReason());
        return ResponseEntity.ok(Map.of("message", "却下しました"));
    }
}
