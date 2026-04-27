package com.example.kintai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateAccountRequest {

    private String loginId;      // 社員番号（8桁、管理者指定）
    private String employeeName;
    private String department;
}
