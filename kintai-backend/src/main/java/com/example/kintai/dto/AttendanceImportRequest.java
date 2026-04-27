package com.example.kintai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceImportRequest {

    private String fileName;
    private List<Map<String, String>> records;
    private String csvContent;
}
