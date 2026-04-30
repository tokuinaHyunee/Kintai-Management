package com.example.kintai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlySummaryDto {

    private int workDays;
    private double totalHours;
    private double overtimeHours;
    private double paidLeaveDays;       // 今月の有給取得日数
    private double remainingAnnualLeave; // 今年の残年次有給日数
}
