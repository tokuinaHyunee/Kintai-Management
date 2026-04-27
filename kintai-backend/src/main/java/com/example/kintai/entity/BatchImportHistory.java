package com.example.kintai.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "batch_import_history")
@Getter
@Setter
@NoArgsConstructor
public class BatchImportHistory {

    //取込ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "import_id")
    private Long importId;

    //ファイル名
    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    //処理結果（SUCCESS / ERROR）
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    //エラー内容
    @Column(name = "error_message", length = 255)
    private String errorMessage;

    //取込日時
    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;
}