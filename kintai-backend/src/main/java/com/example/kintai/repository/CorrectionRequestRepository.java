package com.example.kintai.repository;

import com.example.kintai.entity.CorrectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, Long> {

    List<CorrectionRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<CorrectionRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    void deleteByEmployeeEmployeeId(Long employeeId);
}
