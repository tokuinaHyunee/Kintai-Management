package com.example.kintai.controller;

import com.example.kintai.dto.AttendanceImportRequest;
import com.example.kintai.dto.AttendanceRecordDto;
import com.example.kintai.dto.WorkMemoRequest;
import com.example.kintai.entity.CsvSubmission;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.GoOutRecord;
import com.example.kintai.entity.LeaveRequest;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.CsvSubmissionRepository;
import com.example.kintai.repository.GoOutRecordRepository;
import com.example.kintai.repository.LeaveRequestRepository;
import com.example.kintai.repository.WorkTimeRepository;
import com.example.kintai.util.DateTimeUtil;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final WorkTimeRepository workTimeRepository;
    private final CsvSubmissionRepository csvSubmissionRepository;
    private final GoOutRecordRepository goOutRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeResolver employeeResolver;

    // =========================
    // 一覧取得
    // =========================

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AttendanceRecordDto>> getList(@RequestParam String month) {
        Employee employee = employeeResolver.resolve();
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        Map<LocalDate, AttendanceRecordDto> byDate = new LinkedHashMap<>();
        for (WorkTime wt : workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), start, end)) {
            List<GoOutRecord> goOuts = goOutRecordRepository
                    .findByWorkTimeWorkIdOrderByOutTimeAsc(wt.getWorkId());
            byDate.put(wt.getWorkDate(), AttendanceRecordDto.from(wt, goOuts));
        }

        // 承認済み休暇をマージ
        for (LeaveRequest lr : leaveRequestRepository
                .findByEmployeeEmployeeIdAndStatusAndLeaveDateBetween(
                        employee.getEmployeeId(), "APPROVED", start, end)) {
            byDate.computeIfAbsent(lr.getLeaveDate(), date -> {
                AttendanceRecordDto d = new AttendanceRecordDto();
                d.setWorkDate(date.toString());
                d.setGoOutRecords(List.of());
                return d;
            }).setLeaveType(lr.getLeaveType());
        }

        return ResponseEntity.ok(
                byDate.values().stream()
                        .sorted(Comparator.comparing(AttendanceRecordDto::getWorkDate))
                        .collect(Collectors.toList())
        );
    }

    // =========================
    // 出勤
    // =========================

    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn() {
        Employee employee = employeeResolver.resolve();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt != null && wt.getStartTime() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "本日はすでに出勤打刻済みです"));
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
        Employee employee = employeeResolver.resolve();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getStartTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "出勤打刻がありません"));
        }
        if (wt.getEndTime() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "すでに退勤済みです"));
        }

        LocalTime end = LocalTime.now().withSecond(0).withNano(0);
        int[] calc = DateTimeUtil.calcWorkTime(wt.getStartTime(), end, null);

        wt.setEndTime(end);
        wt.setBreakMinutes(calc[0]);
        wt.setWorkMinutes(calc[1]);
        wt.setOvertimeMinutes(calc[2]);
        workTimeRepository.save(wt);
        return ResponseEntity.ok(Map.of("message", "退勤打刻が完了しました"));
    }

    // =========================
    // 外出（1日複数回対応）
    // =========================

    @PostMapping("/go-out")
    @Transactional
    public ResponseEntity<?> goOut() {
        Employee employee = employeeResolver.resolve();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getStartTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "出勤打刻がありません"));
        }
        if (wt.getEndTime() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "退勤済みです"));
        }
        if (goOutRecordRepository
                .findTopByWorkTimeWorkIdAndReturnTimeIsNullOrderByOutTimeDesc(wt.getWorkId())
                .isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "すでに外出中です"));
        }

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        GoOutRecord record = new GoOutRecord();
        record.setWorkTime(wt);
        record.setOutTime(now);
        goOutRecordRepository.save(record);

        wt.setOutTime(now);
        wt.setReturnTime(null);
        workTimeRepository.save(wt);
        return ResponseEntity.ok(Map.of("message", "外出打刻が完了しました"));
    }

    // =========================
    // 外出戻り（1日複数回対応）
    // =========================

    @PostMapping("/go-out-return")
    @Transactional
    public ResponseEntity<?> goOutReturn() {
        Employee employee = employeeResolver.resolve();
        LocalDate today = LocalDate.now();

        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), today)
                .orElse(null);

        if (wt == null || wt.getStartTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "出勤打刻がありません"));
        }

        GoOutRecord active = goOutRecordRepository
                .findTopByWorkTimeWorkIdAndReturnTimeIsNullOrderByOutTimeDesc(wt.getWorkId())
                .orElse(null);
        if (active == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "外出打刻がありません"));
        }

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        active.setReturnTime(now);
        goOutRecordRepository.save(active);

        wt.setReturnTime(now);
        workTimeRepository.save(wt);
        return ResponseEntity.ok(Map.of("message", "外出戻り打刻が完了しました"));
    }

    // =========================
    // メモ
    // =========================

    @PostMapping("/work-memo")
    public ResponseEntity<?> saveMemo(@RequestBody WorkMemoRequest request) {
        Employee employee = employeeResolver.resolve();
        WorkTime wt = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDate(
                        employee.getEmployeeId(), LocalDate.parse(request.getWork_date()))
                .orElse(null);

        if (wt == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "勤務記録がありません"));
        }
        wt.setWorkMemo(request.getMemo());
        workTimeRepository.save(wt);
        return ResponseEntity.ok(Map.of("message", "保存しました"));
    }

    // =========================
    // CSV管理者送信
    // =========================

    @PostMapping("/submit-csv")
    public ResponseEntity<?> submitCsv(@RequestBody AttendanceImportRequest req) {
        Employee employee = employeeResolver.resolve();

        String csvContent = req.getCsvContent();
        if (csvContent == null || csvContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "データがありません"));
        }

        long lineCount = Arrays.stream(csvContent.split("\\r?\\n"))
                .filter(l -> !l.trim().isEmpty()).count();
        int recordCount = (int) Math.max(0, lineCount - 1);

        CsvSubmission submission = new CsvSubmission();
        submission.setEmployee(employee);
        submission.setFileName(req.getFileName() != null ? req.getFileName() : "submission.csv");
        submission.setRecordsJson(csvContent);
        submission.setRecordCount(recordCount);
        submission.setStatus("PENDING");
        submission.setSubmittedAt(LocalDateTime.now());
        csvSubmissionRepository.save(submission);

        return ResponseEntity.ok(Map.of("message", "管理者に送信しました", "count", recordCount));
    }

    // =========================
    // CSVインポート（レガシー・未使用）
    // =========================

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importAttendance(@RequestBody AttendanceImportRequest req) {
        Employee employee = employeeResolver.resolve();
        List<Map<String, String>> records = req.getRecords();
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "データがありません"));
        }

        int success = 0, error = 0;
        for (Map<String, String> row : records) {
            try {
                LocalDate date  = DateTimeUtil.parseDate(row.get("workDate"));
                LocalTime start = DateTimeUtil.parseTime(row.get("startTime"));
                LocalTime end   = DateTimeUtil.parseTime(row.get("endTime"));
                if (date == null || start == null) { error++; continue; }

                int[] calc = end != null ? DateTimeUtil.calcWorkTime(start, end, null) : new int[]{0, 0, 0};

                WorkTime wt = workTimeRepository
                        .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), date)
                        .orElse(new WorkTime());
                wt.setEmployee(employee);
                wt.setWorkDate(date);
                wt.setStartTime(start);
                wt.setEndTime(end);
                wt.setBreakMinutes(calc[0]);
                wt.setWorkMinutes(end != null ? calc[1] : null);
                wt.setOvertimeMinutes(end != null ? calc[2] : null);
                if (wt.getCreatedAt() == null) wt.setCreatedAt(LocalDateTime.now());
                workTimeRepository.save(wt);
                success++;
            } catch (Exception e) {
                error++;
            }
        }
        return ResponseEntity.ok(Map.of("successCount", success, "errorCount", error, "errors", List.of()));
    }
}
