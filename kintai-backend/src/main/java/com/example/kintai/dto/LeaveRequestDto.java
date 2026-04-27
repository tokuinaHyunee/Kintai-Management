package com.example.kintai.dto;

import com.example.kintai.entity.LeaveRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestDto {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private String leaveTypeName;
    private String leaveDate;
    private String reason;
    private String status;
    private String statusName;
    private String rejectReason;
    private String createdAt;
    private String reviewedAt;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> LEAVE_TYPE_NAMES = Map.of(
        "ANNUAL",         "年次有給休暇",
        "HALF_MORNING",   "午前半休",
        "HALF_AFTERNOON", "午後半休",
        "SICK",           "病気休暇"
    );

    private static final Map<String, String> STATUS_NAMES = Map.of(
        "PENDING",  "申請中",
        "APPROVED", "承認済み",
        "REJECTED", "却下"
    );

    public static LeaveRequestDto from(LeaveRequest r) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.id             = r.getId();
        dto.employeeId     = r.getEmployee().getEmployeeId();
        dto.employeeName   = r.getEmployee().getEmployeeName();
        dto.leaveType      = r.getLeaveType();
        dto.leaveTypeName  = LEAVE_TYPE_NAMES.getOrDefault(r.getLeaveType(), r.getLeaveType());
        dto.leaveDate      = r.getLeaveDate().toString();
        dto.reason         = r.getReason();
        dto.status         = r.getStatus();
        dto.statusName     = STATUS_NAMES.getOrDefault(r.getStatus(), r.getStatus());
        dto.rejectReason   = r.getRejectReason();
        dto.createdAt      = r.getCreatedAt().format(DT_FMT);
        dto.reviewedAt     = r.getReviewedAt() != null ? r.getReviewedAt().format(DT_FMT) : null;
        return dto;
    }
}
