package edu.hit.library.borrow.repo;

import edu.hit.library.borrow.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserIdOrderByBorrowTimeDesc(Long userId);

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);
}
