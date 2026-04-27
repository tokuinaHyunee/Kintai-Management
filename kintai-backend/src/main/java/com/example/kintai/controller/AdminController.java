package com.example.kintai.controller;

import com.example.kintai.dto.EmployeeMonthlySummaryDto;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.EmployeeRepository;
import com.example.kintai.repository.WorkTimeRepository;
import com.example.kintai.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/monthly-summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmployeeMonthlySummaryDto>> getMonthlySummary(@RequestParam String month) {
        YearMonth ym = YearMonth.parse(month);

        List<EmployeeMonthlySummaryDto> result = employeeRepository.findByActiveFlag(1).stream().map(emp -> {
            List<WorkTime> records = workTimeRepository
                    .findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                            emp.getEmployeeId(), ym.atDay(1), ym.atEndOfMonth());

            String loginId = accountRepository.findByEmployeeEmployeeId(emp.getEmployeeId())
                    .map(Account::getLoginId).orElse("—");

            return new EmployeeMonthlySummaryDto(
                    loginId,
                    emp.getEmployeeName(),
                    emp.getDepartment(),
                    DateTimeUtil.countWorkDays(records),
                    DateTimeUtil.toHours(DateTimeUtil.sumWorkMinutes(records)),
                    DateTimeUtil.toHours(DateTimeUtil.sumOvertimeMinutes(records)));
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
