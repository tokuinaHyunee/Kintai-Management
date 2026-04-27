package com.example.kintai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "csv_submission")
@Getter
@Setter
@NoArgsConstructor
public class CsvSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String fileName;

    // パースされたレコードをJSONで保存
    @Column(columnDefinition = "TEXT", nullable = false)
    private String recordsJson;

    @Column(nullable = false)
    private Integer recordCount;

    // PENDING / IMPORTED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime processedAt;
}
