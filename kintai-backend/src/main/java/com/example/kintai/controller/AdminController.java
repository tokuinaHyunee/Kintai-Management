package com.example.kintai.controller;

import com.example.kintai.dto.EmployeeMonthlySummaryDto;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.EmployeeRepository;
import com.example.kintai.repository.WorkTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final WorkTimeRepository workTimeRepository;
    private final AccountRepository accountRepository;

    // 全社員月次集計  GET /api/admin/monthly-summary?month=2026-04
    @GetMapping("/monthly-summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmployeeMonthlySummaryDto>> getMonthlySummary(
            @RequestParam String month) {

        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Employee> employees = employeeRepository.findByActiveFlag(1);

        List<EmployeeMonthlySummaryDto> result = employees.stream().map(emp -> {
            List<WorkTime> records = workTimeRepository
                    .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                            emp.getEmployeeId(), start, end);

            int workDays = (int) records.stream()
                    .filter(r -> r.getWorkMinutes() != null).count();
            int totalMin = records.stream()
                    .filter(r -> r.getWorkMinutes() != null)
                    .mapToInt(WorkTime::getWorkMinutes).sum();
            int overtimeMin = records.stream()
                    .filter(r -> r.getOvertimeMinutes() != null)
                    .mapToInt(WorkTime::getOvertimeMinutes).sum();

            double totalHours = Math.round(totalMin / 60.0 * 10) / 10.0;
            double overtimeHours = Math.round(overtimeMin / 60.0 * 10) / 10.0;

            // 社員番号（loginId）照会
            String loginId = accountRepository.findByEmployeeEmployeeId(emp.getEmployeeId())
                    .map(Account::getLoginId)
                    .orElse("—");

            return new EmployeeMonthlySummaryDto(
                    loginId,
                    emp.getEmployeeName(),
                    emp.getDepartment(),
                    workDays,
                    totalHours,
                    overtimeHours);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
