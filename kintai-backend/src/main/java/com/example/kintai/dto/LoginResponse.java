package com.example.kintai.dto;

/**
 * ログインレスポンス DTO
 */
public class LoginResponse {

    private String token;
    private Long employeeId;
    private String loginId;
    private String employeeName;
    private String role;
    private long expiresIn;

    public LoginResponse(String token, Long employeeId, String loginId,
            String employeeName, String role, long expiresIn) {
        this.token = token;
        this.employeeId = employeeId;
        this.loginId = loginId;
        this.employeeName = employeeName;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public Long getEmployeeId() { return employeeId; }
    public String getLoginId() { return loginId; }
    public String getEmployeeName() { return employeeName; }
    public String getRole() { return role; }
    public long getExpiresIn() { return expiresIn; }
}