package com.project.gigachat.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for deleting account (DELETE /api/user/me).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAccountRequest {
    
    @NotBlank(message = "Password is required to delete account")
    private String password;
}
