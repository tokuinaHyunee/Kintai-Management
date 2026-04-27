package com.example.kintai.repository;

import com.example.kintai.entity.GoOutRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoOutRecordRepository extends JpaRepository<GoOutRecord, Long> {

    List<GoOutRecord> findByWorkTimeWorkIdOrderByOutTimeAsc(Long workId);

    Optional<GoOutRecord> findTopByWorkTimeWorkIdAndReturnTimeIsNullOrderByOutTimeDesc(Long workId);
}
