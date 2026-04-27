package com.example.kintai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlySummaryDto {

    private int workDays;
    private double totalHours;
    private double overtimeHours;
    private int paidLeaveDays;
}
