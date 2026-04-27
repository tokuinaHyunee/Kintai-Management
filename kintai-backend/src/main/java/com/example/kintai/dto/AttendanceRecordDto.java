package com.example.kintai.dto;

import com.example.kintai.entity.GoOutRecord;
import com.example.kintai.entity.WorkTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<GoOutRecordDto> goOutRecords;
    private String leaveType;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GoOutRecordDto {
        private String outTime;
        private String returnTime;

        public static GoOutRecordDto from(GoOutRecord g) {
            GoOutRecordDto dto = new GoOutRecordDto();
            dto.outTime    = g.getOutTime().format(TIME_FMT);
            dto.returnTime = g.getReturnTime() != null ? g.getReturnTime().format(TIME_FMT) : null;
            return dto;
        }
    }

    public static AttendanceRecordDto from(WorkTime w, List<GoOutRecord> goOuts) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.workDate        = w.getWorkDate().toString();
        dto.startTime       = w.getStartTime()    != null ? w.getStartTime().format(TIME_FMT)    : null;
        dto.endTime         = w.getEndTime()       != null ? w.getEndTime().format(TIME_FMT)       : null;
        dto.workMinutes     = w.getWorkMinutes();
        dto.overtimeMinutes = w.getOvertimeMinutes();
        dto.workMemo        = w.getWorkMemo();
        dto.goOutRecords    = goOuts.stream().map(GoOutRecordDto::from).collect(Collectors.toList());

        // outTime / returnTime は現在の外出状態を示す（最後の外出記録から）
        if (!goOuts.isEmpty()) {
            GoOutRecord last = goOuts.get(goOuts.size() - 1);
            dto.outTime    = last.getOutTime().format(TIME_FMT);
            dto.returnTime = last.getReturnTime() != null ? last.getReturnTime().format(TIME_FMT) : null;
        }
        return dto;
    }

    // 後方互換用（GoOutRecord なしで呼ぶ場合）
    public static AttendanceRecordDto from(WorkTime w) {
        return from(w, List.of());
    }
}
