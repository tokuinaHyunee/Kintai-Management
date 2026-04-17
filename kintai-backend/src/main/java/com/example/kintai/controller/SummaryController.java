package com.example.kintai.controller;

import com.example.kintai.dto.MonthlySummaryDto;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.WorkTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// 集計コントローラー
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final WorkTimeRepository workTimeRepository;
    private final AccountRepository accountRepository;

    // 現在ログイン中のEmployeeを取得
    private Employee getCurrentEmployee() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + loginId));
        return account.getEmployee();
    }

    // 月次集計取得  GET /api/summary/monthly?month=2026-04
    @GetMapping("/monthly")
    @Transactional(readOnly = true)
    public ResponseEntity<MonthlySummaryDto> getMonthly(
            @RequestParam(name = "month") String month) {

        Employee employee = getCurrentEmployee();
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), start, end);

        int workDays = (int) records.stream()
                .filter(r -> r.getWorkMinutes() != null)
                .count();

        int totalMinutes = records.stream()
                .filter(r -> r.getWorkMinutes() != null)
                .mapToInt(WorkTime::getWorkMinutes)
                .sum();

        int overtimeMinutes = records.stream()
                .filter(r -> r.getOvertimeMinutes() != null)
                .mapToInt(WorkTime::getOvertimeMinutes)
                .sum();

        double totalHours = Math.round(totalMinutes / 60.0 * 10) / 10.0;
        double overtimeHours = Math.round(overtimeMinutes / 60.0 * 10) / 10.0;

        return ResponseEntity.ok(
                new MonthlySummaryDto(workDays, totalHours, overtimeHours, 10)
        );
    }

    // 週次集計取得  GET /api/summary/weekly
    @GetMapping("/weekly")
    @Transactional(readOnly = true)
    public ResponseEntity<MonthlySummaryDto> getWeekly(
            @RequestParam(name = "month", required = false) String month) {

        Employee employee = getCurrentEmployee();
        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(java.time.DayOfWeek.SUNDAY);

        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), weekStart, weekEnd);

        int workDays = (int) records.stream()
                .filter(r -> r.getWorkMinutes() != null)
                .count();

        int totalMinutes = records.stream()
                .filter(r -> r.getWorkMinutes() != null)
                .mapToInt(WorkTime::getWorkMinutes)
                .sum();

        int overtimeMinutes = records.stream()
                .filter(r -> r.getOvertimeMinutes() != null)
                .mapToInt(WorkTime::getOvertimeMinutes)
                .sum();

        double totalHours = Math.round(totalMinutes / 60.0 * 10) / 10.0;
        double overtimeHours = Math.round(overtimeMinutes / 60.0 * 10) / 10.0;

        return ResponseEntity.ok(
                new MonthlySummaryDto(workDays, totalHours, overtimeHours, 0)
        );
    }

    // CSVエクスポート  GET /api/summary/export?month=2026-04
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "month") String month) {

        Employee employee = getCurrentEmployee();
        YearMonth ym = YearMonth.parse(month);

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), start, end);

        StringBuilder csv = new StringBuilder();
        csv.append("日付,出勤,退勤,外出,外出戻り,労働時間(分),残業時間(分),業務内容\n");

        for (WorkTime r : records) {
            csv.append(r.getWorkDate()).append(",");
            csv.append(r.getStartTime() != null ? r.getStartTime() : "").append(",");
            csv.append(r.getEndTime() != null ? r.getEndTime() : "").append(",");
            csv.append(r.getOutTime() != null ? r.getOutTime() : "").append(",");
            csv.append(r.getReturnTime() != null ? r.getReturnTime() : "").append(",");
            csv.append(r.getWorkMinutes() != null ? r.getWorkMinutes() : "").append(",");
            csv.append(r.getOvertimeMinutes() != null ? r.getOvertimeMinutes() : "").append(",");
            csv.append(r.getWorkMemo() != null ? r.getWorkMemo().replace(",", "、") : "");
            csv.append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"attendance_" + month + ".csv\"")
                .body(bytes);
    }
}