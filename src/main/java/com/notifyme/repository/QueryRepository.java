package com.notifyme.repository;

import com.notifyme.entity.Query;
import com.notifyme.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query as JpaQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    
    List<Query> findByUser(User user);
    
    List<Query> findByUserAndIsValid(User user, Boolean isValid);
    
    List<Query> findByIsValidAndNextExecutionBefore(Boolean isValid, LocalDateTime dateTime);
    
    List<Query> findByUserOrderByCreatedAtDesc(User user);
    
    // Query per trovare query per tipo
    List<Query> findByUserAndCronTrue(User user);
    
    List<Query> findByUserAndDateSpecificTrue(User user);
    
    List<Query> findByUserAndToCheckTrue(User user);
    
    // Query per trovare query attive (non chiuse e valide)
    @JpaQuery("SELECT q FROM Query q WHERE q.user = :user AND q.isValid = true AND (q.closed = false OR q.closed IS NULL)")
    List<Query> findActiveQueriesByUser(@Param("user") User user);
    
    // Query per trovare query pronte per l'esecuzione con validità temporale
    @JpaQuery("SELECT q FROM Query q WHERE q.isValid = true AND q.nextExecution <= :now AND " +
             "(q.closed = false OR q.closed IS NULL) AND " +
             "(q.validFrom IS NULL OR q.validFrom <= :now) AND " +
             "(q.validTo IS NULL OR q.validTo >= :now)")
    List<Query> findQueriesReadyForExecutionWithValidityPeriod(@Param("now") LocalDateTime now);
    
    // Query per statistiche
    @JpaQuery("SELECT COUNT(q) FROM Query q WHERE q.user = :user AND q.isValid = true")
    Long countValidQueriesByUser(@Param("user") User user);
    
    @JpaQuery("SELECT COUNT(q) FROM Query q WHERE q.user = :user AND q.cron = true")
    Long countCronQueriesByUser(@Param("user") User user);
    
    @JpaQuery("SELECT COUNT(q) FROM Query q WHERE q.user = :user AND q.dateSpecific = true")
    Long countSpecificQueriesByUser(@Param("user") User user);
    
    @JpaQuery("SELECT COUNT(q) FROM Query q WHERE q.user = :user AND q.toCheck = true")
    Long countCheckQueriesByUser(@Param("user") User user);
}