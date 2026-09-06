package com.payflow.payflow.repository;

import com.payflow.payflow.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserWalletIdOrderByCreatedAtDesc(UUID userWalletId);
}