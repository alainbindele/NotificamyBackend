package com.notifyme.repository;

import com.notifyme.entity.TExecution;
import com.notifyme.entity.TQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<TExecution, Long> {
    
    List<TExecution> findByQuery(TQuery query);
    
    List<TExecution> findByQueryOrderByExecutedAtDesc(TQuery query);
}