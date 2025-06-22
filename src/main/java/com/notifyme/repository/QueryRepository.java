package com.notifyme.repository;

import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    
    List<Query> findByUser(User user);
    
    List<Query> findByUserAndIsValid(User user, Boolean isValid);
    
    List<Query> findByIsValidAndNextExecutionBefore(Boolean isValid, LocalDateTime dateTime);
    
    List<Query> findByUserOrderByCreatedAtDesc(User user);
}