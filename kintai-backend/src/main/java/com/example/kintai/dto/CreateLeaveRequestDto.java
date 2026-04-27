package com.example.kintai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateLeaveRequestDto {
    private String leaveType; // ANNUAL / HALF_MORNING / HALF_AFTERNOON / SICK
    private String leaveDate; // yyyy-MM-dd
    private String reason;
}
