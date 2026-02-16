package com.project.gigachat.controller;

import com.project.gigachat.dto.user.*;
import com.project.gigachat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User management controller.
 * - /api/user/* — Authenticated user's own profile operations
 * - /api/users/* — User discovery and search
 */
@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * GET /api/user/me — Get current user's full profile.
     */
    @GetMapping("/api/user/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
    
    /**
     * PATCH /api/user/me — Update profile (displayName, email).
     */
    @PatchMapping("/api/user/me")
    public ResponseEntity<UpdateProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
        
    /**
     * POST /api/user/avatar — Request a pre-signed URL for avatar upload.
     * Frontend uses the returned uploadUrl to PUT the image directly to S3.
     */
    @PostMapping("/api/user/avatar")
    public ResponseEntity<AvatarUploadResponse> requestAvatarUpload(@Valid @RequestBody AvatarUploadRequest request) {
        return ResponseEntity.ok(userService.requestAvatarUpload(request));
    }
    
    /**
     * DELETE /api/user/me — Delete account (requires password confirmation).
     */
    @DeleteMapping("/api/user/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(request);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
    
    /**
     * GET /api/users/search?q=...&limit=10 — Search users by username only.
     */
    @GetMapping("/api/users/search")
    public ResponseEntity<UserSearchResponse> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.searchUsers(query, limit));
    }
}
