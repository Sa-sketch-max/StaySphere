package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository
        extends JpaRepository<WebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}
