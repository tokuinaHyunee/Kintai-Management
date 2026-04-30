package com.example.kintai.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_time", indexes = {
    @Index(name = "idx_wt_emp_date", columnList = "employee_id, work_date")
})
@Getter
@Setter
@NoArgsConstructor
public class WorkTime {

    // 勤怠ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_id")
    private Long workId;

    // 社員（FK）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // 勤務日
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // 出勤時刻
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // 退勤時刻
    @Column(name = "end_time")
    private LocalTime endTime;

    // 外出時刻
    @Column(name = "out_time")
    private LocalTime outTime;

    // 外出戻り時刻
    @Column(name = "return_time")
    private LocalTime returnTime;

    // 休憩時間（分）
    @Column(name = "break_minutes", nullable = false)
    private Integer breakMinutes;

    // 実働時間（分）
    @Column(name = "work_minutes")
    private Integer workMinutes;

    // 業務内容メモ
    @Column(name = "work_memo", length = 500)
    private String workMemo;

    // 残業時間（分）
    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    // 外出記録（1日複数回対応）
    @OneToMany(mappedBy = "workTime", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("outTime ASC")
    private List<GoOutRecord> goOutRecords = new ArrayList<>();

    // 登録日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
