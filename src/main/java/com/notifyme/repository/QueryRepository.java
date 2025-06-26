package com.notifyme.repository;

import com.notifyme.entity.TQuery;
import com.notifyme.entity.TUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryRepository extends JpaRepository<TQuery, Long> {
    
    List<TQuery> findByUser(TUser TUser);
    
    List<TQuery> findByUserAndIsValid(TUser TUser, Boolean isValid);
    
    List<TQuery> findByIsValidAndNextExecutionBefore(Boolean isValid, LocalDateTime dateTime);
    
    List<TQuery> findByUserOrderByCreatedAtDesc(TUser TUser);
    
    // Query per trovare query per tipo
    List<TQuery> findByUserAndCronTrue(TUser TUser);
    
    List<TQuery> findByUserAndDateSpecificTrue(TUser TUser);
    
    List<TQuery> findByUserAndToCheckTrue(TUser TUser);
    
    // Query per trovare query attive (non chiuse e valide)
    @Query("SELECT q FROM TQuery q WHERE q.user = :user AND q.isValid = true AND (q.closed = false OR q.closed IS NULL)")
    List<TQuery> findActiveQueriesByUser(@Param("user") TUser TUser);
    
    // Query per trovare query pronte per l'esecuzione con validità temporale
    @Query("SELECT q FROM TQuery q WHERE q.isValid = true AND q.nextExecution <= :now AND " +
             "(q.closed = false OR q.closed IS NULL) AND " +
             "(q.validFrom IS NULL OR q.validFrom <= :now) AND " +
             "(q.validTo IS NULL OR q.validTo >= :now)")
    List<TQuery> findQueriesReadyForExecutionWithValidityPeriod(@Param("now") LocalDateTime now);
    
    // Query per statistiche
    @Query("SELECT COUNT(q) FROM TQuery q WHERE q.user = :user AND q.isValid = true")
    Long countValidQueriesByUser(@Param("user") TUser TUser);
    
    @Query("SELECT COUNT(q) FROM TQuery q WHERE q.user = :user AND q.cron = true")
    Long countCronQueriesByUser(@Param("user") TUser TUser);
    
    @Query("SELECT COUNT(q) FROM TQuery q WHERE q.user = :user AND q.dateSpecific = true")
    Long countSpecificQueriesByUser(@Param("user") TUser TUser);
    
    @Query("SELECT COUNT(q) FROM TQuery q WHERE q.user = :user AND q.toCheck = true")
    Long countCheckQueriesByUser(@Param("user") TUser TUser);
}