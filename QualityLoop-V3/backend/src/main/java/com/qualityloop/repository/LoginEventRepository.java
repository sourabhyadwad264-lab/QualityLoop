package com.qualityloop.repository;

import com.qualityloop.entity.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    List<LoginEvent> findTop100ByOrderByAtDesc();
}
