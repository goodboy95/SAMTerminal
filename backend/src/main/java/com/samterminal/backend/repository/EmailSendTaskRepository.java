package com.samterminal.backend.repository;

import com.samterminal.backend.entity.EmailSendTask;
import com.samterminal.backend.entity.EmailSendTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface EmailSendTaskRepository extends JpaRepository<EmailSendTask, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EmailSendTask t where t.status in :statuses and t.nextAttemptAt <= :now order by t.nextAttemptAt asc")
    List<EmailSendTask> findReadyTasks(@Param("statuses") List<EmailSendTaskStatus> statuses,
                                       @Param("now") LocalDateTime now);

    EmailSendTask findTopByRequestIdOrderByCreatedAtDesc(String requestId);
}
