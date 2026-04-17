package com.example.kintai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ログインリクエスト DTO
 */
public class LoginRequest {

    @NotBlank(message = "ログインIDは必須です")
    private String loginId;

    @NotBlank(message = "パスワードは必須です")
    private String password;

    public LoginRequest() {}

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}