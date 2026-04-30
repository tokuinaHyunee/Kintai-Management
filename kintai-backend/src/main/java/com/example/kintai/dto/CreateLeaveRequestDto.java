package com.example.kintai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateLeaveRequestDto {
    @NotBlank(message = "休暇種別は必須です")
    private String leaveType; // ANNUAL / HALF_MORNING / HALF_AFTERNOON / SICK

    @NotBlank(message = "休暇取得日は必須です")
    private String leaveDate; // yyyy-MM-dd

    private String reason;
}
