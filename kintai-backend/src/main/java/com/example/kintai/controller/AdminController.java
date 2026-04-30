package com.example.kintai.controller;

import com.example.kintai.dto.EmployeeMonthlySummaryDto;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.WorkTime;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.WorkTimeRepository;
import com.example.kintai.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WorkTimeRepository workTimeRepository;
    private final AccountRepository accountRepository;

    // N+1 解消: アクティブ社員アカウント1回 + 勤務記録1回 = 計2クエリ
    @GetMapping("/monthly-summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmployeeMonthlySummaryDto>> getMonthlySummary(@RequestParam String month) {
        YearMonth ym = YearMonth.parse(month);

        List<Account> accounts = accountRepository.findActiveWithEmployee();

        Map<Long, List<WorkTime>> byEmpId = workTimeRepository
                .findWithEmployeeByWorkDateBetween(ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .collect(Collectors.groupingBy(w -> w.getEmployee().getEmployeeId()));

        List<EmployeeMonthlySummaryDto> result = accounts.stream().map(acc -> {
            Long empId = acc.getEmployee().getEmployeeId();
            List<WorkTime> records = byEmpId.getOrDefault(empId, List.of());
            return new EmployeeMonthlySummaryDto(
                    acc.getLoginId(),
                    acc.getEmployee().getEmployeeName(),
                    acc.getEmployee().getDepartment(),
                    DateTimeUtil.countWorkDays(records),
                    DateTimeUtil.toHours(DateTimeUtil.sumWorkMinutes(records)),
                    DateTimeUtil.toHours(DateTimeUtil.sumOvertimeMinutes(records)));
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
