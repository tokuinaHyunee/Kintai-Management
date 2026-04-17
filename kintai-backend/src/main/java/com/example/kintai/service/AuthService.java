package com.example.kintai.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.kintai.dto.LoginRequest;
import com.example.kintai.dto.LoginResponse;
import com.example.kintai.entity.Account;
import com.example.kintai.repository.AccountRepository;
import com.example.kintai.security.JwtTokenProvider;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new RuntimeException("社員番号またはパスワードが正しくありません"));

        if (account.getActiveFlag() != 1) {
            throw new RuntimeException("無効なアカウントです");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("社員番号またはパスワードが正しくありません");
        }

        String token = jwtTokenProvider.generateToken(
                account.getLoginId(),
                account.getRole()
        );

        return new LoginResponse(
                token,
                account.getEmployee().getEmployeeId(),
                account.getLoginId(),
                account.getEmployee().getEmployeeName(),
                account.getRole(),
                1800L
        );
    }

    public String refreshToken(String loginId, String role) {
        return jwtTokenProvider.generateToken(loginId, role);
    }
}
