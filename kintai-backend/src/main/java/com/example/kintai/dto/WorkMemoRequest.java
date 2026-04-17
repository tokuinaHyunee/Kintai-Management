package com.example.kintai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkMemoRequest {

    private String work_date;
    private String memo;
}
