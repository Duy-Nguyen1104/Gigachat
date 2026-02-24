package com.project.gigachat.service;

import com.project.gigachat.dto.user.AvatarUploadRequest;
import com.project.gigachat.dto.user.AvatarUploadResponse;
import com.project.gigachat.dto.user.DeleteAccountRequest;
import com.project.gigachat.dto.user.UpdateProfileRequest;
import com.project.gigachat.dto.user.UpdateProfileResponse;
import com.project.gigachat.dto.user.UserProfileResponse;
import com.project.gigachat.dto.user.UserSearchResponse;
import com.project.gigachat.dto.user.UserSearchResult;
import com.project.gigachat.entity.User;
import com.project.gigachat.exception.BadRequestException;
import com.project.gigachat.exception.ConflictException;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * GET /api/user/me — Get the full profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return toProfileResponse(user);
    }
    
    /**
     * PATCH /api/user/me — Update the authenticated user's profile (displayName, email).
     */
    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        
        User saved = userRepository.save(user);
        log.info("User profile updated: {}", saved.getId());
        
        return UpdateProfileResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .displayName(saved.getDisplayName())
                .avatarUrl(s3Service.generatePresignedGetUrl(saved.getAvatarUrl()))
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
    
    /**
     * POST /api/user/avatar — Generate a pre-signed URL for avatar upload.
     * The frontend will use this URL to upload the image directly to S3.
     * After a successful upload, the permanent file URL is stored on the user's profile.
     */
    @Transactional
    public AvatarUploadResponse requestAvatarUpload(AvatarUploadRequest request) {
        User user = securityUtils.getCurrentUser();
        
        // Delete old avatar from S3 if one exists
        if (user.getAvatarUrl() != null) {
            String oldKey = s3Service.extractKeyFromUrl(user.getAvatarUrl());
            if (oldKey != null) {
                s3Service.deleteObject(oldKey);
            }
        }
        
        // Generate the pre-signed upload URL
        S3Service.PresignedUploadResult result = s3Service.generateAvatarUploadUrl(
                user.getId(), request.getFileName(), request.getFileType());
        
        // Store the raw S3 key — bucket stays private; use presigned GET to view
        user.setAvatarUrl(result.key());
        userRepository.save(user);
        
        log.info("Avatar upload URL generated for user: {}", user.getId());
        
        return AvatarUploadResponse.builder()
                .uploadUrl(result.uploadUrl())
                .viewUrl(s3Service.generatePresignedGetUrl(result.key()))
                .expiresInMinutes(result.expiresInMinutes())
                .build();
    }
    
    /**
     * DELETE /api/user/me — Delete the authenticated user's account.
     */
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        User user = securityUtils.getCurrentUser();
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }
        
        // Delete avatar from S3 if one exists
        if (user.getAvatarUrl() != null) {
            String key = s3Service.extractKeyFromUrl(user.getAvatarUrl());
            if (key != null) {
                s3Service.deleteObject(key);
            }
        }
        
        userRepository.delete(user);
        log.info("User account deleted: {}", user.getId());
    }
    
    /**
     * GET /api/users/search?q=...&limit=... — Search users by username only.
     * Excludes the current user from results. Minimum query length: 2 characters.
     */
    @Transactional(readOnly = true)
    public UserSearchResponse searchUsers(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters");
        }
        
        User currentUser = securityUtils.getCurrentUser();
        
        // Fetch one extra to determine hasMore
        List<User> users = userRepository.searchUsers(
                query.trim(), currentUser.getId(), PageRequest.of(0, limit + 1));
        
        boolean hasMore = users.size() > limit;
        List<UserSearchResult> results = users.stream()
                .limit(limit)
                .map(u -> UserSearchResult.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName())
                        .avatarUrl(s3Service.generatePresignedGetUrl(u.getAvatarUrl()))
                        .build())
                .toList();
        
        return UserSearchResponse.builder()
                .users(results)
                .hasMore(hasMore)
                .build();
    }
    
    // ---- Private helpers ----
    
    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(s3Service.generatePresignedGetUrl(user.getAvatarUrl()))
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
