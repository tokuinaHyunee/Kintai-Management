package com.example.kintai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeMonthlySummaryDto {

    private String loginId;       // 社員番号兼ログインID
    private String employeeName;
    private String department;
    private int workDays;
    private double totalHours;
    private double overtimeHours;
}
