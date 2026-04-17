package com.example.kintai.dto;

import com.example.kintai.entity.Account;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountListDto {

    private Long accountId;
    private String employeeName;
    private String department;
    private String loginId;        // 社員番号兼ログインID
    private String passwordPlain;  // 管理者確認用平文パスワード
    private String role;
    private Integer activeFlag;

    public static AccountListDto from(Account a) {
        AccountListDto dto = new AccountListDto();
        dto.accountId = a.getAccountId();
        dto.employeeName = a.getEmployee().getEmployeeName();
        dto.department = a.getEmployee().getDepartment();
        dto.loginId = a.getLoginId();
        dto.passwordPlain = a.getPasswordPlain();
        dto.role = a.getRole();
        dto.activeFlag = a.getActiveFlag();
        return dto;
    }
}
