package com.example.kintai.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.kintai.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	
	//ログインIDで検索（Spring Security認証用）
	Optional<Account> findByLoginId(String loginId);
	
	//社員IDで検索
	Optional<Account> findByEmployeeEmployeeId(Long employeeId);
}