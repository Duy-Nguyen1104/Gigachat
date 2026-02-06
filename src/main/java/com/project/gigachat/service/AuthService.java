package com.project.gigachat.service;

import com.project.gigachat.dto.auth.*;
import com.project.gigachat.entity.PasswordResetToken;
import com.project.gigachat.entity.RefreshToken;
import com.project.gigachat.entity.User;
import com.project.gigachat.repository.PasswordResetTokenRepository;
import com.project.gigachat.repository.RefreshTokenRepository;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .build();
        
        user = userRepository.save(user);
        
        // Generate tokens
        String accessToken = tokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
        // Save refresh token
        saveRefreshToken(user, refreshToken);
        
        return AuthResponse.builder()
                .user(mapToUserInfo(user))
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Get user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update last seen
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate tokens
        String accessToken = tokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
        // Save refresh token
        saveRefreshToken(user, refreshToken);
        
        return AuthResponse.builder()
                .user(mapToUserInfo(user))
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Transactional
    public void logout(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenString = request.getRefreshToken();
        
        // Validate refresh token
        if (!tokenProvider.validateToken(refreshTokenString)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // Get refresh token from database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        
        // Check if token is revoked or expired
        if (refreshToken.getRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }
        
        // Get user
        User user = refreshToken.getUser();
        
        // Generate new tokens
        String newAccessToken = tokenProvider.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        
        // Save new refresh token
        saveRefreshToken(user, newRefreshToken);
        
        return AuthResponse.builder()
                .user(mapToUserInfo(user))
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
    
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
        
        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .token(resetToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        passwordResetTokenRepository.save(passwordResetToken);
        
        // TODO: Send email with reset token
        // For now, just log it
        System.out.println("Password reset token: " + resetToken);
    }
    
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
        
        // Check if token is used or expired
        if (resetToken.getUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invalid or expired token");
        }
        
        // Update user password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
    
    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        refreshTokenRepository.save(refreshToken);
    }
    
    private UserInfo mapToUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
