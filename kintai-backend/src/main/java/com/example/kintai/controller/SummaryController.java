package com.example.kintai.controller;

import com.example.kintai.dto.MonthlySummaryDto;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.WorkTimeRepository;
import com.example.kintai.util.DateTimeUtil;
import com.example.kintai.util.EmployeeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final WorkTimeRepository workTimeRepository;
    private final EmployeeResolver employeeResolver;

    @GetMapping("/monthly")
    @Transactional(readOnly = true)
    public ResponseEntity<MonthlySummaryDto> getMonthly(@RequestParam String month) {
        var employee = employeeResolver.resolve();
        YearMonth ym = YearMonth.parse(month);
        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), ym.atDay(1), ym.atEndOfMonth());

        return ResponseEntity.ok(new MonthlySummaryDto(
                DateTimeUtil.countWorkDays(records),
                DateTimeUtil.toHours(DateTimeUtil.sumWorkMinutes(records)),
                DateTimeUtil.toHours(DateTimeUtil.sumOvertimeMinutes(records)),
                10));
    }

    @GetMapping("/weekly")
    @Transactional(readOnly = true)
    public ResponseEntity<MonthlySummaryDto> getWeekly(@RequestParam(required = false) String month) {
        var employee = employeeResolver.resolve();
        LocalDate today = LocalDate.now();
        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(),
                        today.with(java.time.DayOfWeek.MONDAY),
                        today.with(java.time.DayOfWeek.SUNDAY));

        return ResponseEntity.ok(new MonthlySummaryDto(
                DateTimeUtil.countWorkDays(records),
                DateTimeUtil.toHours(DateTimeUtil.sumWorkMinutes(records)),
                DateTimeUtil.toHours(DateTimeUtil.sumOvertimeMinutes(records)),
                0));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam String month) {
        var employee = employeeResolver.resolve();
        YearMonth ym = YearMonth.parse(month);
        List<WorkTime> records = workTimeRepository
                .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                        employee.getEmployeeId(), ym.atDay(1), ym.atEndOfMonth());

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
