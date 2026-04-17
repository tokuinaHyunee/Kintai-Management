package com.example.kintai.controller;

import com.example.kintai.dto.CorrectionRequestDto;
import com.example.kintai.dto.CreateCorrectionRequestDto;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.CorrectionRequest;
import com.example.kintai.entity.Employee;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.CorrectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 修正申請コントローラー
@RestController
@RequiredArgsConstructor
public class CorrectionRequestController {

    private final CorrectionRequestRepository correctionRequestRepository;
    private final AccountRepository accountRepository;

    // 現在ログイン中のEmployeeを取得
    private Employee getCurrentEmployee() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + loginId));
        return account.getEmployee();
    }

    // 修正申請登録  POST /api/correction-requests
    @PostMapping("/api/correction-requests")
    public ResponseEntity<?> create(@RequestBody CreateCorrectionRequestDto dto) {
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "申請理由を入力してください"));
        }

        Employee employee = getCurrentEmployee();

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

    // 管理者：申請一覧取得  GET /api/admin/correction-requests?status=PENDING
    @GetMapping("/api/admin/correction-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CorrectionRequestDto>> getList(
            @RequestParam(required = false) String status) {

        List<CorrectionRequest> list = (status != null && !status.isBlank())
                ? correctionRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : correctionRequestRepository.findAllByOrderByCreatedAtDesc();

        return ResponseEntity.ok(list.stream()
                .map(CorrectionRequestDto::new)
                .collect(Collectors.toList()));
    }

    // 管理者：未読件数  GET /api/admin/correction-requests/unread-count
    @GetMapping("/api/admin/correction-requests/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = correctionRequestRepository.countByStatus("PENDING");
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 管理者：既読にする  PUT /api/admin/correction-requests/{id}/read
    @PutMapping("/api/admin/correction-requests/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        CorrectionRequest req = correctionRequestRepository.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        req.setStatus("READ");
        correctionRequestRepository.save(req);
        return ResponseEntity.ok(Map.of("message", "既読にしました"));
    }
}
