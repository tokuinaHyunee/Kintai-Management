package com.example.kintai.dto;

import com.example.kintai.entity.CorrectionRequest;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class CorrectionRequestDto {

    private final Long   id;
    private final String employeeName;
    private final String workDate;
    private final String requestType;
    private final String newStartTime;
    private final String newEndTime;
    private final String reason;
    private final String status;
    private final String createdAt;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CorrectionRequestDto(CorrectionRequest r) {
        this.id           = r.getId();
        this.employeeName = r.getEmployee().getEmployeeName();
        this.workDate     = r.getWorkDate().toString();
        this.requestType  = r.getRequestType();
        this.newStartTime = r.getNewStartTime() != null ? r.getNewStartTime().format(TIME_FMT) : null;
        this.newEndTime   = r.getNewEndTime()   != null ? r.getNewEndTime().format(TIME_FMT)   : null;
        this.reason       = r.getReason();
        this.status       = r.getStatus();
        this.createdAt    = r.getCreatedAt().format(DT_FMT);
    }
}
