package com.example.kintai.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.kintai.entity.WorkTime;

@Repository
public interface WorkTimeRepository extends JpaRepository<WorkTime, Long> {
	
	//社員IDと勤務日で検索（重複チェック用）
	Optional<WorkTime> findByEmployeeEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
	
	//社員IDで勤務記録一覧取得
	List<WorkTime> findByEmployeeEmployeeIdOrderByWorkDateDesc(Long employeeId);
	//社員IDと期間で勤務記録取得（月次集計用）
	List<WorkTime> findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, LocalDate startDate, LocalDate endDate);
	
	//全社員の特定期間の勤務記録取得（管理者用）
	List<WorkTime> findByWorkDateBetweenOrderByWorkDateAsc(
            LocalDate startDate, LocalDate endDate);

	// 社員の全勤務記録を削除（アカウント完全削除用）
	void deleteByEmployeeEmployeeId(Long employeeId);
}