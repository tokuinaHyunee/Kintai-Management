package com.example.kintai.controller;

import com.example.kintai.dto.AttendanceImportRequest;
import com.example.kintai.dto.AttendanceRecordDto;
import com.example.kintai.dto.WorkMemoRequest;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.BatchImportHistory;
import com.example.kintai.entity.CsvSubmission;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.BatchImportHistoryRepository;
import com.example.kintai.repository.CsvSubmissionRepository;
import com.example.kintai.repository.WorkTimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final WorkTimeRepository workTimeRepository;
    private final AccountRepository accountRepository;
    private final BatchImportHistoryRepository batchImportHistoryRepository;
    private final CsvSubmissionRepository csvSubmissionRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("H:mm"),
    };

    // =========================
    // 共通
    // =========================

    private Employee getCurrentEmployee() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + loginId));
        return account.getEmployee();
    }

    private LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private LocalTime parseTime(String s) {
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try { return LocalTime.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // =========================
    // 一覧取得
    // =========================

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AttendanceRecordDto>> getList(@RequestParam String month) {
        Employee employee = getCurrentEmployee();
        YearMonth ym = YearMonth.parse(month);

        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(),
                        ym.atDay(1),
                        ym.atEndOfMonth()
                );

        return ResponseEntity.ok(
                records.stream().map(AttendanceRecordDto::from).collect(Collectors.toList())
        );
    }

    // =========================
    // 出勤
    // =========================

    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn() {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt != null && wt.getStartTime() != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "本日はすでに出勤打刻済みです"));
        }

        if (wt == null) {
            wt = new WorkTime();
            wt.setEmployee(employee);
            wt.setWorkDate(today);
            wt.setCreatedAt(LocalDateTime.now());
        }

        wt.setStartTime(LocalTime.now().withSecond(0).withNano(0));
        wt.setBreakMinutes(0);

        workTimeRepository.save(wt);

        return ResponseEntity.ok(Map.of("message", "出勤打刻が完了しました"));
    }

    // =========================
    // 退勤
    // =========================

    @PostMapping("/clock-out")
    public ResponseEntity<?> clockOut() {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getStartTime() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "出勤打刻がありません"));
        }

        if (wt.getEndTime() != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "すでに退勤済みです"));
        }

        LocalTime end = LocalTime.now().withSecond(0).withNano(0);

        int total = (int) Duration.between(wt.getStartTime(), end).toMinutes();
        int breakMin = total > 360 ? 60 : 0;
        int workMin = Math.max(0, total - breakMin);
        int overtime = Math.max(0, workMin - 480);

        wt.setEndTime(end);
        wt.setBreakMinutes(breakMin);
        wt.setWorkMinutes(workMin);
        wt.setOvertimeMinutes(overtime);

        workTimeRepository.save(wt);

        return ResponseEntity.ok(Map.of("message", "退勤打刻が完了しました"));
    }

    // =========================
    // 外出
    // =========================

    @PostMapping("/go-out")
    public ResponseEntity<?> goOut() {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getStartTime() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "出勤打刻がありません"));
        }

        if (wt.getEndTime() != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "退勤済みです"));
        }

        if (wt.getOutTime() != null && wt.getReturnTime() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "すでに外出中です"));
        }

        wt.setOutTime(LocalTime.now().withSecond(0).withNano(0));
        wt.setReturnTime(null);

        workTimeRepository.save(wt);

        return ResponseEntity.ok(Map.of("message", "外出打刻が完了しました"));
    }

    // =========================
    // 外出戻り
    // =========================

    @PostMapping("/go-out-return")
    public ResponseEntity<?> goOutReturn() {
        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getOutTime() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "外出打刻がありません"));
        }

        if (wt.getReturnTime() != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "すでに戻っています"));
        }

        wt.setReturnTime(LocalTime.now().withSecond(0).withNano(0));

        workTimeRepository.save(wt);

        return ResponseEntity.ok(Map.of("message", "外出戻り打刻が完了しました"));
    }

    // =========================
    // メモ
    // =========================

    @PostMapping("/work-memo")
    public ResponseEntity<?> saveMemo(@RequestBody WorkMemoRequest request) {
        Employee employee = getCurrentEmployee();
        LocalDate date = LocalDate.parse(request.getWork_date());

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), date)
                .orElse(null);

        if (wt == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "勤務記録がありません"));
        }

        wt.setWorkMemo(request.getMemo());
        workTimeRepository.save(wt);

        return ResponseEntity.ok(Map.of("message", "保存しました"));
    }

    // =========================
    // CSV管理者送信（社員 → 管理者メールボックス）
    // =========================

    @PostMapping("/submit-csv")
    public ResponseEntity<?> submitCsv(@RequestBody AttendanceImportRequest req) {
        Employee employee = getCurrentEmployee();

        String csvContent = req.getCsvContent();
        if (csvContent == null || csvContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "データがありません"));
        }

        try {
            // 非空行数をカウントしてレコード件数の目安とする
            long lineCount = Arrays.stream(csvContent.split("\\r?\\n"))
                .filter(l -> !l.trim().isEmpty())
                .count();
            int recordCount = (int) Math.max(0, lineCount - 1);

            CsvSubmission submission = new CsvSubmission();
            submission.setEmployee(employee);
            submission.setFileName(req.getFileName() != null ? req.getFileName() : "submission.csv");
            submission.setRecordsJson(csvContent);
            submission.setRecordCount(recordCount);
            submission.setStatus("PENDING");
            submission.setSubmittedAt(LocalDateTime.now());
            csvSubmissionRepository.save(submission);

            return ResponseEntity.ok(Map.of(
                "message", "管理者に送信しました",
                "count",   recordCount
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "送信に失敗しました"));
        }
    }

    // =========================
    // CSVインポート（レガシー: 直接取込 - 現在未使用）
    // =========================

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importAttendance(
            @RequestBody AttendanceImportRequest req) {

        Employee employee = getCurrentEmployee();

        List<Map<String, String>> records = req.getRecords();
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "データがありません"));
        }

        int success = 0;
        int error = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, String> row : records) {
            try {
                LocalDate date = parseDate(row.get("workDate"));
                LocalTime start = parseTime(row.get("startTime"));
                LocalTime end = parseTime(row.get("endTime"));

                if (date == null || start == null) {
                    error++;
                    continue;
                }

                int total = end != null ? (int) Duration.between(start, end).toMinutes() : 0;
                int breakMin = total > 360 ? 60 : 0;
                int workMin = Math.max(0, total - breakMin);
                int overtime = Math.max(0, workMin - 480);

                WorkTime wt = workTimeRepository
                        .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), date)
                        .orElse(new WorkTime());

                wt.setEmployee(employee);
                wt.setWorkDate(date);
                wt.setStartTime(start);
                wt.setEndTime(end);
                wt.setBreakMinutes(breakMin);
                wt.setWorkMinutes(workMin);
                wt.setOvertimeMinutes(overtime);
                if (wt.getCreatedAt() == null) wt.setCreatedAt(LocalDateTime.now());

                workTimeRepository.save(wt);
                success++;

            } catch (Exception e) {
                error++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "successCount", success,
                "errorCount", error,
                "errors", errors
        ));
    }
}