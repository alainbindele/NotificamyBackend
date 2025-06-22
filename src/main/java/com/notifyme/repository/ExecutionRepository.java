package com.notifyme.repository;

import com.notifyme.entity.Execution;
import com.notifyme.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    
    List<Execution> findByQuery(Query query);
    
    List<Execution> findByQueryOrderByExecutedAtDesc(Query query);
}