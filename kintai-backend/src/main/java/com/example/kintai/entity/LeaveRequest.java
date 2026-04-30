package com.example.kintai.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_request", indexes = {
    @Index(name = "idx_lr_emp_status_date", columnList = "employee_id, status, leave_date"),
    @Index(name = "idx_lr_status_created",  columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 申請者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // 休暇種別: ANNUAL / HALF_MORNING / HALF_AFTERNOON / SICK
    @Column(name = "leave_type", nullable = false, length = 20)
    private String leaveType;

    // 休暇取得日
    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    // 申請理由（任意）
    @Column(name = "reason", length = 500)
    private String reason;

    // ステータス: PENDING / APPROVED / REJECTED
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    // 却下理由（却下時のみ）
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // 申請日時
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 審査日時
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
