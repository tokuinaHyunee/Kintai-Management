package com.example.kintai.controller;

import com.example.kintai.dto.CorrectionRequestDto;
import com.example.kintai.dto.CreateCorrectionRequestDto;
import com.example.kintai.entity.CorrectionRequest;
import com.example.kintai.repository.CorrectionRequestRepository;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CorrectionRequestController {

    private final CorrectionRequestRepository correctionRequestRepository;
    private final EmployeeResolver employeeResolver;

    @PostMapping("/api/correction-requests")
    public ResponseEntity<?> create(@RequestBody CreateCorrectionRequestDto dto) {
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "申請理由を入力してください"));
        }

        var employee = employeeResolver.resolve();
        CorrectionRequest req = new CorrectionRequest();
        req.setEmployee(employee);
        req.setWorkDate(LocalDate.parse(dto.getWorkDate()));
        req.setRequestType(dto.getRequestType());
        if (dto.getNewStartTime() != null && !dto.getNewStartTime().isBlank())
            req.setNewStartTime(LocalTime.parse(dto.getNewStartTime()));
        if (dto.getNewEndTime() != null && !dto.getNewEndTime().isBlank())
            req.setNewEndTime(LocalTime.parse(dto.getNewEndTime()));
        req.setReason(dto.getReason());
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());

        correctionRequestRepository.save(req);
        return ResponseEntity.ok(Map.of("message", "申請を受け付けました"));
    }

    @GetMapping("/api/admin/correction-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CorrectionRequestDto>> getList(@RequestParam(required = false) String status) {
        List<CorrectionRequest> list = (status != null && !status.isBlank())
                ? correctionRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : correctionRequestRepository.findAllByOrderByCreatedAtDesc();

        return ResponseEntity.ok(list.stream()
                .map(CorrectionRequestDto::new)
                .collect(Collectors.toList()));
    }

    @GetMapping("/api/admin/correction-requests/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", correctionRequestRepository.countByStatus("PENDING")));
    }

    @PutMapping("/api/admin/correction-requests/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        CorrectionRequest req = correctionRequestRepository.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        req.setStatus("READ");
        correctionRequestRepository.save(req);
        return ResponseEntity.ok(Map.of("message", "既読にしました"));
    }
}
