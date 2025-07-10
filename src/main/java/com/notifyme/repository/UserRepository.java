package com.notifyme.repository;

import com.notifyme.entity.TUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<TUser, Long> {
    
    Optional<TUser> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<TUser> findByAuthSubject(String authSubject);
    
    boolean existsByAuthSubject(String authSubject);
}