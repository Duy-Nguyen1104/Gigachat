package com.project.gigachat.repository;

import com.project.gigachat.entity.RefreshToken;
import com.project.gigachat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUser(User user);
    
    void deleteByExpiresAtBefore(LocalDateTime now);
}
