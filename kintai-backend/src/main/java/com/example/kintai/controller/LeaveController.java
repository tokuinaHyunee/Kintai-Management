package com.example.kintai.controller;

import com.example.kintai.dto.CreateLeaveRequestDto;
import com.example.kintai.dto.LeaveRequestDto;
import com.example.kintai.dto.RejectLeaveRequestDto;
import com.example.kintai.entity.LeaveRequest;
import com.example.kintai.repository.LeaveRequestRepository;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeResolver employeeResolver;

    private static final Set<String> VALID_LEAVE_TYPES =
            Set.of("ANNUAL", "HALF_MORNING", "HALF_AFTERNOON", "SICK");

    @PostMapping("/api/leaves")
    public ResponseEntity<?> applyLeave(@RequestBody CreateLeaveRequestDto req) {
        if (req.getLeaveType() == null || !VALID_LEAVE_TYPES.contains(req.getLeaveType())) {
            return ResponseEntity.badRequest().body(Map.of("message", "休暇種別が無効です"));
        }
        if (req.getLeaveDate() == null || req.getLeaveDate().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "休暇取得日を入力してください"));
        }

        LocalDate leaveDate;
        try {
            leaveDate = LocalDate.parse(req.getLeaveDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "日付の形式が無効です（yyyy-MM-dd）"));
        }

        var employee = employeeResolver.resolve();
        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setLeaveType(req.getLeaveType());
        lr.setLeaveDate(leaveDate);
        lr.setReason(req.getReason());
        lr.setStatus("PENDING");
        lr.setCreatedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);

        return ResponseEntity.ok(Map.of("message", "休暇申請を送信しました"));
    }

    @GetMapping("/api/leaves")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LeaveRequestDto>> getMyLeaves() {
        var employee = employeeResolver.resolve();
        return ResponseEntity.ok(
                leaveRequestRepository
                        .findByEmployeeEmployeeIdOrderByCreatedAtDesc(employee.getEmployeeId())
                        .stream().map(LeaveRequestDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/api/admin/leaves")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LeaveRequestDto>> getAllLeaves(@RequestParam(required = false) String status) {
        List<LeaveRequest> list = (status != null && !status.isBlank())
                ? leaveRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(list.stream().map(LeaveRequestDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/api/admin/leaves/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", leaveRequestRepository.countByStatus("PENDING")));
    }

    @PutMapping("/api/admin/leaves/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id) {
        LeaveRequest lr = leaveRequestRepository.findById(id).orElse(null);
        if (lr == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(lr.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message", "申請中の休暇のみ承認できます"));
        lr.setStatus("APPROVED");
        lr.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
        return ResponseEntity.ok(Map.of("message", "承認しました"));
    }

    @PutMapping("/api/admin/leaves/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody RejectLeaveRequestDto req) {
        if (req.getRejectReason() == null || req.getRejectReason().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "却下理由を入力してください"));
        LeaveRequest lr = leaveRequestRepository.findById(id).orElse(null);
        if (lr == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(lr.getStatus()))
            return ResponseEntity.badRequest().body(Map.of("message", "申請中の休暇のみ却下できます"));
        lr.setStatus("REJECTED");
        lr.setRejectReason(req.getRejectReason());
        lr.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
        return ResponseEntity.ok(Map.of("message", "却下しました"));
    }
}
