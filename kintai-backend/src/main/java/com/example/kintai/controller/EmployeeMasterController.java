package com.example.kintai.controller;

import com.example.kintai.dto.AccountListDto;
import com.example.kintai.dto.CreateAccountRequest;
import com.example.kintai.entity.Account;
import com.example.kintai.entity.Employee;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.repository.CorrectionRequestRepository;
import com.example.kintai.repository.CsvSubmissionRepository;
import com.example.kintai.repository.EmployeeRepository;
import com.example.kintai.repository.LeaveRequestRepository;
import com.example.kintai.repository.WorkTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class EmployeeMasterController {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkTimeRepository workTimeRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final CsvSubmissionRepository csvSubmissionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(PW_CHARS.charAt(RANDOM.nextInt(PW_CHARS.length())));
        }
        return sb.toString();
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountListDto>> getAccounts() {
        List<AccountListDto> result = accountRepository.findAll().stream()
                .map(AccountListDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest req) {
        if (req.getLoginId() == null || req.getLoginId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "社員番号を入力してください"));
        }
        if (req.getLoginId().length() != 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "社員番号は8桁で入力してください"));
        }
        if (req.getEmployeeName() == null || req.getEmployeeName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "氏名を入力してください"));
        }
        if (accountRepository.findByLoginId(req.getLoginId()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "社員番号はすでに使用されています: " + req.getLoginId()));
        }

        String plainPassword = generatePassword();

        Employee emp = new Employee();
        emp.setEmployeeName(req.getEmployeeName());
        emp.setDepartment(req.getDepartment());
        emp.setActiveFlag(1);
        emp.setCreatedAt(LocalDateTime.now());
        emp.setUpdatedAt(LocalDateTime.now());
        emp = employeeRepository.save(emp);

        Account acc = new Account();
        acc.setEmployee(emp);
        acc.setLoginId(req.getLoginId());
        acc.setPassword(passwordEncoder.encode(plainPassword));
        acc.setPasswordPlain(plainPassword);
        acc.setRole("USER");
        acc.setActiveFlag(1);
        acc.setCreatedAt(LocalDateTime.now());
        acc.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(acc);

        return ResponseEntity.ok(Map.of(
                "message", "社員を登録しました",
                "loginId", req.getLoginId(),
                "password", plainPassword
        ));
    }

    @DeleteMapping("/accounts/{id}")
    @Transactional
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        Account acc = accountRepository.findById(id).orElse(null);
        if (acc == null) {
            return ResponseEntity.notFound().build();
        }
        if ("ADMIN".equals(acc.getRole())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "管理者アカウントは削除できません"));
        }

        Long empId = acc.getEmployee().getEmployeeId();
        leaveRequestRepository.deleteByEmployeeEmployeeId(empId);
        correctionRequestRepository.deleteByEmployeeEmployeeId(empId);
        csvSubmissionRepository.deleteByEmployeeEmployeeId(empId);
        workTimeRepository.deleteByEmployeeEmployeeId(empId);
        accountRepository.delete(acc);
        employeeRepository.deleteById(empId);

        return ResponseEntity.ok(Map.of("message", "社員を削除しました"));
    }
}
