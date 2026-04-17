package com.example.kintai.dto;

import com.example.kintai.entity.WorkTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecordDto {

    private String workDate;
    private String startTime;
    private String endTime;
    private String outTime;
    private String returnTime;
    private Integer workMinutes;
    private Integer overtimeMinutes;
    private String workMemo;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static AttendanceRecordDto from(WorkTime w) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.workDate       = w.getWorkDate().toString();
        dto.startTime      = w.getStartTime()   != null ? w.getStartTime().format(TIME_FMT)   : null;
        dto.endTime        = w.getEndTime()      != null ? w.getEndTime().format(TIME_FMT)      : null;
        dto.outTime        = w.getOutTime()      != null ? w.getOutTime().format(TIME_FMT)      : null;
        dto.returnTime     = w.getReturnTime()   != null ? w.getReturnTime().format(TIME_FMT)   : null;
        dto.workMinutes    = w.getWorkMinutes();
        dto.overtimeMinutes = w.getOvertimeMinutes();
        dto.workMemo       = w.getWorkMemo();
        return dto;
    }
}
