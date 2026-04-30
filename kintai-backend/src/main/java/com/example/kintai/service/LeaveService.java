package com.example.kintai.service;

import com.example.kintai.dto.CreateLeaveRequestDto;
import com.example.kintai.dto.LeaveRequestDto;
import com.example.kintai.entity.Employee;
import com.example.kintai.entity.LeaveRequest;
import com.example.kintai.exception.BusinessException;
import com.example.kintai.exception.ResourceNotFoundException;
import com.example.kintai.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final Set<String> VALID_LEAVE_TYPES =
            Set.of("ANNUAL", "HALF_MORNING", "HALF_AFTERNOON", "SICK");

    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    public void apply(Employee employee, CreateLeaveRequestDto req) {
        if (req.getLeaveType() == null || !VALID_LEAVE_TYPES.contains(req.getLeaveType())) {
            throw new BusinessException("休暇種別が無効です");
        }
        if (req.getLeaveDate() == null || req.getLeaveDate().isBlank()) {
            throw new BusinessException("休暇取得日を入力してください");
        }

        LocalDate leaveDate;
        try {
            leaveDate = LocalDate.parse(req.getLeaveDate());
        } catch (Exception e) {
            throw new BusinessException("日付の形式が無効です（yyyy-MM-dd）");
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setLeaveType(req.getLeaveType());
        lr.setLeaveDate(leaveDate);
        lr.setReason(req.getReason());
        lr.setStatus("PENDING");
        lr.setCreatedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getMyLeaves(Employee employee) {
        return leaveRequestRepository
                .findByEmployeeEmployeeIdOrderByCreatedAtDesc(employee.getEmployeeId())
                .stream().map(LeaveRequestDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getAllLeaves(String status) {
        List<LeaveRequest> list = (status != null && !status.isBlank())
                ? leaveRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(LeaveRequestDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return leaveRequestRepository.countByStatus("PENDING");
    }

    @Transactional
    public void approve(Long id) {
        LeaveRequest lr = leaveRequestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("申請が見つかりません"));
        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("申請中の休暇のみ承認できます");
        }
        lr.setStatus("APPROVED");
        lr.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
    }

    @Transactional
    public void reject(Long id, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new BusinessException("却下理由を入力してください");
        }
        LeaveRequest lr = leaveRequestRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("申請が見つかりません"));
        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("申請中の休暇のみ却下できます");
        }
        lr.setStatus("REJECTED");
        lr.setRejectReason(rejectReason);
        lr.setReviewedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
    }
}
