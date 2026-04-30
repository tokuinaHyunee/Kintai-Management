package com.example.kintai.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.kintai.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByLoginId(String loginId);

    Optional<Account> findByEmployeeEmployeeId(Long employeeId);

    // 管理者月次集計用: アクティブ社員のアカウントを Employee ごと JOIN FETCH
    @Query("SELECT a FROM Account a JOIN FETCH a.employee e WHERE e.activeFlag = 1 ORDER BY e.employeeName ASC")
    List<Account> findActiveWithEmployee();
}