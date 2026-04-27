package com.example.kintai.util;

import com.example.kintai.entity.Employee;
import com.example.kintai.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * JWT から現在ログイン中の Employee を取得する共通コンポーネント。
 * 各コントローラーで重複していた getCurrentEmployee() を集約。
 */
@Component
@RequiredArgsConstructor
public class EmployeeResolver {

    private final AccountRepository accountRepository;

    public Employee resolve() {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + loginId))
                .getEmployee();
    }
}
