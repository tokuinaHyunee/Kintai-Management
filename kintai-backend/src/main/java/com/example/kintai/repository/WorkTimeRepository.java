package com.example.kintai.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.kintai.entity.WorkTime;

@Repository
public interface WorkTimeRepository extends JpaRepository<WorkTime, Long> {

    Optional<WorkTime> findByEmployeeEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<WorkTime> findByEmployeeEmployeeIdOrderByWorkDateDesc(Long employeeId);

    List<WorkTime> findByEmployeeEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, LocalDate startDate, LocalDate endDate);

    // N+1 解消: GoOutRecord を JOIN FETCH して1クエリで取得
    @Query("SELECT w FROM WorkTime w LEFT JOIN FETCH w.goOutRecords" +
           " WHERE w.employee.employeeId = :empId" +
           " AND w.workDate BETWEEN :start AND :end" +
           " ORDER BY w.workDate ASC")
    List<WorkTime> findWithGoOutRecordsByEmployeeAndDateBetween(
            @Param("empId") Long empId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // 管理者月次集計用: 全社員の勤務記録を Employee ごと JOIN FETCH (N+1解消)
    @Query("SELECT w FROM WorkTime w JOIN FETCH w.employee" +
           " WHERE w.workDate BETWEEN :start AND :end")
    List<WorkTime> findWithEmployeeByWorkDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    List<WorkTime> findByWorkDateBetweenOrderByWorkDateAsc(
            LocalDate startDate, LocalDate endDate);

    void deleteByEmployeeEmployeeId(Long employeeId);
}