package com.example.kintai.dto;

import com.example.kintai.entity.CsvSubmission;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class CsvSubmissionDto {

    private final Long    id;
    private final String  employeeName;
    private final String  fileName;
    private final Integer recordCount;
    private final String  status;
    private final String  submittedAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CsvSubmissionDto(CsvSubmission s) {
        this.id           = s.getId();
        this.employeeName = s.getEmployee().getEmployeeName();
        this.fileName     = s.getFileName();
        this.recordCount  = s.getRecordCount();
        this.status       = s.getStatus();
        this.submittedAt  = s.getSubmittedAt().format(FMT);
    }
}
