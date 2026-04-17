package com.example.kintai.controller;

import com.example.kintai.dto.AttendanceImportRequest;
import com.example.kintai.entity.BatchImportHistory;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.BatchImportHistoryRepository;
import com.example.kintai.repository.EmployeeRepository;
import com.example.kintai.repository.WorkTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class ImportController {

    private final WorkTimeRepository workTimeRepository;
    private final EmployeeRepository employeeRepository;
    private final BatchImportHistoryRepository batchImportHistoryRepository;

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

    // 勤怠データ一括インポート  POST /api/admin/import/attendance
    // CSVのemployeeNameカラムで社員マッチング
    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> importAttendance(
            @RequestBody AttendanceImportRequest req) {

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        List<Map<String, String>> records = req.getRecords();
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "インポートするデータがありません"));
        }

        // 氏名の事前チェック: 存在しない社員名が含まれる場合は即エラー返却
        Set<String> uniqueNames = new LinkedHashSet<>();
        for (Map<String, String> r : records) {
            String n = r.getOrDefault("employeeName", "").trim();
            if (!n.isBlank()) uniqueNames.add(n);
        }
        List<String> missingNames = new ArrayList<>();
        for (String name : uniqueNames) {
            if (employeeRepository.findByEmployeeName(name).isEmpty()) {
                missingNames.add(name);
            }
        }
        if (!missingNames.isEmpty()) {
            String nameList = String.join("、", missingNames);
            return ResponseEntity.badRequest().body(Map.of(
                "message",      "一致する社員名がありません: " + nameList,
                "successCount", 0,
                "errorCount",   records.size(),
                "errors",       List.of("一致する社員名がありません: " + nameList)
            ));
        }

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> row = records.get(i);
            int rowNum = i + 2;

            try {
                // 社員名でマッチング
                String empName = row.get("employeeName");
                if (empName == null || empName.isBlank()) {
                    errors.add(rowNum + "行目: 氏名が空です");
                    errorCount++;
                    continue;
                }

                Employee employee = employeeRepository.findByEmployeeName(empName.trim())
                        .orElse(null);
                if (employee == null) {
                    errors.add(rowNum + "行目: 氏名「" + empName + "」が見つかりません");
                    errorCount++;
                    continue;
                }

                // 勤務日
                String workDateStr = row.get("workDate");
                if (workDateStr == null || workDateStr.isBlank()) {
                    errors.add(rowNum + "行目: 勤務日が空です");
                    errorCount++;
                    continue;
                }
                LocalDate workDate = parseDate(workDateStr.trim());
                if (workDate == null) {
                    errors.add(rowNum + "行目: 勤務日の形式が正しくありません「" + workDateStr + "」");
                    errorCount++;
                    continue;
                }

                // 出勤時刻
                String startTimeStr = row.get("startTime");
                if (startTimeStr == null || startTimeStr.isBlank()) {
                    errors.add(rowNum + "行目: 出勤時刻が空です");
                    errorCount++;
                    continue;
                }
                LocalTime startTime = parseTime(startTimeStr.trim());
                if (startTime == null) {
                    errors.add(rowNum + "行目: 出勤時刻の形式が正しくありません「" + startTimeStr + "」");
                    errorCount++;
                    continue;
                }

                // 退勤時刻（任意）
                LocalTime endTime = null;
                String endTimeStr = row.get("endTime");
                if (endTimeStr != null && !endTimeStr.isBlank()) {
                    endTime = parseTime(endTimeStr.trim());
                    if (endTime == null) {
                        errors.add(rowNum + "行目: 退勤時刻の形式が正しくありません「" + endTimeStr + "」");
                        errorCount++;
                        continue;
                    }
                }

                // 休憩時間（任意、デフォルトは自動計算）
                Integer breakMinutes = 0;
                String breakStr = row.get("breakMinutes");
                if (breakStr != null && !breakStr.isBlank()) {
                    try {
                        breakMinutes = Integer.parseInt(breakStr.trim());
                    } catch (NumberFormatException e) {
                        errors.add(rowNum + "行目: 休憩時間の形式が正しくありません「" + breakStr + "」");
                        errorCount++;
                        continue;
                    }
                }

                String workMemo = row.getOrDefault("workMemo", "");
                if (workMemo != null) workMemo = workMemo.trim();

                // 労働時間 / 残業時間計算
                Integer workMinutes = null;
                Integer overtimeMinutes = null;
                if (endTime != null) {
                    int totalMin = (int) Duration.between(startTime, endTime).toMinutes();
                    if (totalMin < 0) totalMin += 24 * 60;
                    if (breakStr == null || breakStr.isBlank()) {
                        breakMinutes = totalMin > 360 ? 60 : 0;
                    }
                    workMinutes = Math.max(0, totalMin - breakMinutes);
                    overtimeMinutes = Math.max(0, workMinutes - 480);
                }

                // 同日の記録がある場合は上書き
                WorkTime wt = workTimeRepository
                        .findByEmployeeEmployeeIdAndWorkDate(employee.getEmployeeId(), workDate)
                        .orElse(new WorkTime());

                wt.setEmployee(employee);
                wt.setWorkDate(workDate);
                wt.setStartTime(startTime);
                wt.setEndTime(endTime);
                wt.setBreakMinutes(breakMinutes);
                wt.setWorkMinutes(workMinutes);
                wt.setOvertimeMinutes(overtimeMinutes);
                if (workMemo != null && !workMemo.isBlank()) wt.setWorkMemo(workMemo);
                if (wt.getCreatedAt() == null) wt.setCreatedAt(LocalDateTime.now());

                workTimeRepository.save(wt);
                successCount++;

            } catch (Exception e) {
                errors.add(rowNum + "行目: 予期せぬエラー - " + e.getMessage());
                errorCount++;
            }
        }

        BatchImportHistory history = new BatchImportHistory();
        history.setFileName(req.getFileName() != null ? req.getFileName() : "unknown.csv");
        history.setStatus(errorCount == 0 ? "SUCCESS" : (successCount == 0 ? "ERROR" : "PARTIAL"));
        if (!errors.isEmpty()) {
            String errMsg = String.join("; ", errors.subList(0, Math.min(errors.size(), 3)));
            history.setErrorMessage(errMsg.length() > 255 ? errMsg.substring(0, 252) + "..." : errMsg);
        }
        history.setImportedAt(LocalDateTime.now());
        batchImportHistoryRepository.save(history);

        return ResponseEntity.ok(Map.of(
                "successCount", successCount,
                "errorCount", errorCount,
                "errors", errors
        ));
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
}
