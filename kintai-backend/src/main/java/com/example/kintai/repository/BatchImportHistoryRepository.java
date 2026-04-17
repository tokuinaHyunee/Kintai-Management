package com.example.kintai.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.kintai.entity.BatchImportHistory;

@Repository
public interface BatchImportHistoryRepository extends JpaRepository<BatchImportHistory, Long> {

    //処理結果で検索
    List<BatchImportHistory> findByStatusOrderByImportedAtDesc(String status);

    //最新の取込履歴を取得
    List<BatchImportHistory> findTop10ByOrderByImportedAtDesc();
}