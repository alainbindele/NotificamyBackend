package com.notifyme.repository;

import com.notifyme.entity.Notification;
import com.notifyme.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUser(User user);
    
    List<Notification> findByUserOrderBySentAtDesc(User user);
}