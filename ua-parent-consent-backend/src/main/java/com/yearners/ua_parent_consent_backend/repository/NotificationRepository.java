
package com.yearners.ua_parent_consent_backend.repository;

import com.yearners.ua_parent_consent_backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    long countByUserIdAndIsReadFalse(Long userId);
}
