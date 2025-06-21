package com.notifyme.repository;

import com.notifyme.entity.Keychain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeychainRepository extends JpaRepository<Keychain, Integer> {

    @Query("SELECT k FROM Keychain k WHERE k.apikey = :apikey")
    Optional<Keychain> findByApikey(@Param("apikey") String apikey);

    @Query("SELECT k FROM Keychain k WHERE k.apikey = :apikey AND k.expired = false AND k.disabled = false")
    Optional<Keychain> findValidApikey(@Param("apikey") String apikey);
}