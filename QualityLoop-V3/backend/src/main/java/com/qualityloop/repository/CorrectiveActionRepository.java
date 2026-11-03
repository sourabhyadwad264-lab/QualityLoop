package com.qualityloop.repository;

import com.qualityloop.entity.ActionStatus;
import com.qualityloop.entity.CorrectiveAction;
import com.qualityloop.entity.DefectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {

    // JPQL: pending actions whose due date has passed, on defects that are not closed
    @Query("select a from CorrectiveAction a join fetch a.defect d "
         + "where a.status = :pending and a.dueDate < :today and d.status <> :closed "
         + "order by a.dueDate")
    List<CorrectiveAction> findOverdue(@Param("pending") ActionStatus pending,
                                       @Param("closed") DefectStatus closed,
                                       @Param("today") LocalDate today);
}
