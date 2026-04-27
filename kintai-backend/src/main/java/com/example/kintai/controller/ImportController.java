package com.example.kintai.controller;

import com.example.kintai.dto.AttendanceImportRequest;
import com.example.kintai.entity.BatchImportHistory;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.BatchImportHistoryRepository;
import com.example.kintai.repository.EmployeeRepository;
import com.example.kintai.repository.WorkTimeRepository;
import com.example.kintai.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> importAttendance(@RequestBody AttendanceImportRequest req) {
        int successCount = 0, errorCount = 0;
        List<String> errors = new ArrayList<>();

        List<Map<String, String>> records = req.getRecords();
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "インポートするデータがありません"));
        }

        Set<String> uniqueNames = new LinkedHashSet<>();
        for (Map<String, String> r : records) {
            String n = r.getOrDefault("employeeName", "").trim();
            if (!n.isBlank()) uniqueNames.add(n);
        }
        List<String> missingNames = new ArrayList<>();
        for (String name : uniqueNames) {
            if (employeeRepository.findByEmployeeName(name).isEmpty()) missingNames.add(name);
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
                String empName = row.get("employeeName");
                if (empName == null || empName.isBlank()) {
                    errors.add(rowNum + "行目: 氏名が空です"); errorCount++; continue;
                }
                Employee employee = employeeRepository.findByEmployeeName(empName.trim()).orElse(null);
                if (employee == null) {
                    errors.add(rowNum + "行目: 氏名「" + empName + "」が見つかりません"); errorCount++; continue;
                }

                String workDateStr = row.get("workDate");
                if (workDateStr == null || workDateStr.isBlank()) {
                    errors.add(rowNum + "行目: 勤務日が空です"); errorCount++; continue;
                }
                LocalDate workDate = DateTimeUtil.parseDate(workDateStr.trim());
                if (workDate == null) {
                    errors.add(rowNum + "行目: 勤務日の形式が正しくありません「" + workDateStr + "」"); errorCount++; continue;
                }

                String startTimeStr = row.get("startTime");
                if (startTimeStr == null || startTimeStr.isBlank()) {
                    errors.add(rowNum + "行目: 出勤時刻が空です"); errorCount++; continue;
                }
                LocalTime startTime = DateTimeUtil.parseTime(startTimeStr.trim());
                if (startTime == null) {
                    errors.add(rowNum + "行目: 出勤時刻の形式が正しくありません「" + startTimeStr + "」"); errorCount++; continue;
                }

                LocalTime endTime = null;
                String endTimeStr = row.get("endTime");
                if (endTimeStr != null && !endTimeStr.isBlank()) {
                    endTime = DateTimeUtil.parseTime(endTimeStr.trim());
                    if (endTime == null) {
                        errors.add(rowNum + "行目: 退勤時刻の形式が正しくありません「" + endTimeStr + "」"); errorCount++; continue;
                    }
                }

                Integer breakOverride = null;
                String breakStr = row.get("breakMinutes");
                if (breakStr != null && !breakStr.isBlank()) {
                    try {
                        breakOverride = Integer.parseInt(breakStr.trim());
                    } catch (NumberFormatException e) {
                        errors.add(rowNum + "行目: 休憩時間の形式が正しくありません「" + breakStr + "」"); errorCount++; continue;
                    }
                }

                String workMemo = row.getOrDefault("workMemo", "").trim();

                Integer workMinutes = null, overtimeMinutes = null, breakMinutes = 0;
                if (endTime != null) {
                    int[] calc = DateTimeUtil.calcWorkTime(startTime, endTime, breakOverride);
                    breakMinutes = calc[0];
                    workMinutes  = calc[1];
                    overtimeMinutes = calc[2];
                } else if (breakOverride != null) {
                    breakMinutes = breakOverride;
                }

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
                if (!workMemo.isBlank()) wt.setWorkMemo(workMemo);
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

        return ResponseEntity.ok(Map.of("successCount", successCount, "errorCount", errorCount, "errors", errors));
    }
}
