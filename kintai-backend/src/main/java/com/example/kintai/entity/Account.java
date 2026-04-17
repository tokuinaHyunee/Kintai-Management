package com.example.kintai.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    // 社員番号兼ログインID（8桁、管理者指定）
    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    // パスワード（ハッシュ）
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // パスワード平文（管理者確認用）
    @Column(name = "password_plain", length = 20)
    private String passwordPlain;

    // 権限（ADMIN / USER）
    @Column(name = "role", nullable = false, length = 10)
    private String role = "USER";

    // 有効フラグ（1:有効 / 0:無効）
    @Column(name = "active_flag", nullable = false, columnDefinition = "TINYINT")
    private Integer activeFlag = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
