package com.notifyme.repository;

import com.notifyme.entity.TNotification;
import com.notifyme.entity.TUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<TNotification, Long> {
    
    List<TNotification> findByUser(TUser user);
    
    List<TNotification> findByUserOrderBySentAtDesc(TUser user);
}