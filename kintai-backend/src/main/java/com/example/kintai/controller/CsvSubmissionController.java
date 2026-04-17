package com.example.kintai.controller;

import com.example.kintai.dto.CsvSubmissionDto;
import com.example.kintai.entity.CsvSubmission;
import com.example.kintai.repository.CsvSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/csv-submissions")
@RequiredArgsConstructor
public class CsvSubmissionController {

    private final CsvSubmissionRepository csvSubmissionRepository;

    // 未処理件数照会  GET /api/admin/csv-submissions/pending-count
    @GetMapping("/pending-count")
    public ResponseEntity<?> getPendingCount() {
        return ResponseEntity.ok(Map.of("count", csvSubmissionRepository.countByStatus("PENDING")));
    }

    // 一覧照会  GET /api/admin/csv-submissions?status=PENDING
    @GetMapping
    public ResponseEntity<List<CsvSubmissionDto>> getList(
            @RequestParam(required = false, defaultValue = "") String status) {
        List<CsvSubmission> list = status.isBlank()
            ? csvSubmissionRepository.findAllByOrderBySubmittedAtDesc()
            : csvSubmissionRepository.findByStatusOrderBySubmittedAtDesc(status);
        return ResponseEntity.ok(
            list.stream().map(CsvSubmissionDto::new).collect(Collectors.toList())
        );
    }

    // CSV内容取得  GET /api/admin/csv-submissions/{id}/content
    @GetMapping("/{id}/content")
    public ResponseEntity<?> getContent(@PathVariable Long id) {
        CsvSubmission submission = csvSubmissionRepository.findById(id).orElse(null);
        if (submission == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
            "id",           submission.getId(),
            "fileName",     submission.getFileName(),
            "csvContent",   submission.getRecordsJson(),
            "employeeName", submission.getEmployee().getEmployeeName(),
            "status",       submission.getStatus()
        ));
    }

    // 取込済みマーク  POST /api/admin/csv-submissions/{id}/mark-imported
    @PostMapping("/{id}/mark-imported")
    @Transactional
    public ResponseEntity<?> markImported(@PathVariable Long id) {
        CsvSubmission submission = csvSubmissionRepository.findById(id).orElse(null);
        if (submission == null) return ResponseEntity.notFound().build();
        submission.setStatus("IMPORTED");
        submission.setProcessedAt(LocalDateTime.now());
        csvSubmissionRepository.save(submission);
        return ResponseEntity.ok(Map.of("message", "取込済みにしました"));
    }

    // 削除  DELETE /api/admin/csv-submissions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubmission(@PathVariable Long id) {
        if (!csvSubmissionRepository.existsById(id)) return ResponseEntity.notFound().build();
        csvSubmissionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "削除しました"));
    }

}
