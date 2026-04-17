package com.example.kintai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCorrectionRequestDto {
    private String workDate;       // yyyy-MM-dd
    private String requestType;    // START_TIME / END_TIME / BOTH
    private String newStartTime;   // HH:mm (nullable)
    private String newEndTime;     // HH:mm (nullable)
    private String reason;
}
