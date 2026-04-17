package com.example.kintai.repository;

import com.example.kintai.entity.CsvSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsvSubmissionRepository extends JpaRepository<CsvSubmission, Long> {
    List<CsvSubmission> findByStatusOrderBySubmittedAtDesc(String status);
    List<CsvSubmission> findAllByOrderBySubmittedAtDesc();
    long countByStatus(String status);
    void deleteByEmployeeEmployeeId(Long employeeId);
}
