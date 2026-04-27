package com.example.kintai.repository;

import com.example.kintai.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    List<LeaveRequest> findByEmployeeEmployeeIdAndStatusAndLeaveDateBetween(
            Long employeeId, String status, java.time.LocalDate start, java.time.LocalDate end);

    void deleteByEmployeeEmployeeId(Long employeeId);
}
