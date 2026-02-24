package com.project.gigachat.service;

import com.project.gigachat.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private int presignedUrlExpirationMinutes;
    
    @Value("${upload.max-file-size}")
    private long maxFileSize;
    
    @Value("${upload.allowed-image-types}")
    private String allowedImageTypes;
    
    @Value("${upload.allowed-file-types}")
    private String allowedFileTypes;
    
    /**
     * Generate a pre-signed PUT URL for uploading an avatar image.
     *
     * @param userId   the user's UUID (used as folder prefix)
     * @param fileName the original file name
     * @param fileType the MIME type (e.g., image/png)
     * @return PresignedUploadResult containing the upload URL and the final public file URL
     */
    public PresignedUploadResult generateAvatarUploadUrl(UUID userId, String fileName, String fileType) {
        validateImageType(fileType);
        String key = buildKey("avatars", userId, fileName);
        return generatePresignedPutUrl(key, fileType);
    }
    
    /**
     * Generate a pre-signed PUT URL for uploading a chat attachment.
     *
     * @param conversationId the conversation UUID (used as folder prefix)
     * @param senderId       the sender's UUID
     * @param fileName       the original file name
     * @param fileType       the MIME type
     * @return PresignedUploadResult containing the upload URL and the final public file URL
     */
    public PresignedUploadResult generateMessageUploadUrl(UUID conversationId, UUID senderId, String fileName, String fileType) {
        validateFileType(fileType);
        String key = buildKey("messages/" + conversationId, senderId, fileName);
        return generatePresignedPutUrl(key, fileType);
    }
    
    /**
     * Delete an object from S3 by its key.
     */
    public void deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            log.info("Deleted S3 object: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to delete S3 object: {}", key, e);
            throw new FileUploadException("Failed to delete file from storage", e);
        }
    }
    
    /**
     * Extract the S3 object key from a full S3 URL.
     * Example: https://bucket.s3.region.amazonaws.com/avatars/uuid/file.png → avatars/uuid/file.png
     */
    public String extractKeyFromUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }
        // Handle legacy full URL: https://{bucket}.s3.{region}.amazonaws.com/{key}
        if (keyOrUrl.startsWith("https://")) {
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            return keyOrUrl.startsWith(prefix) ? keyOrUrl.substring(prefix.length()) : null;
        }
        // Already a raw S3 key
        return keyOrUrl;
    }

    /**
     * Generate a pre-signed GET URL for viewing a private S3 object.
     * Accepts either a raw S3 key (new format) or a legacy full URL.
     * Returns null if the input is null/blank or unresolvable.
     */
    public String generatePresignedGetUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }
        String key = extractKeyFromUrl(keyOrUrl);
        if (key == null) {
            return null;
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (S3Exception e) {
            log.error("Failed to generate pre-signed GET URL for key: {}", key, e);
            throw new FileUploadException("Failed to generate view URL", e);
        }
    }
    
    // ---- Private helpers ----
    
    private PresignedUploadResult generatePresignedPutUrl(String key, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
            
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();
            
            String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

            log.debug("Generated pre-signed upload URL for key: {}", key);

            return new PresignedUploadResult(uploadUrl, key, presignedUrlExpirationMinutes);
        } catch (S3Exception e) {
            log.error("Failed to generate pre-signed URL for key: {}", key, e);
            throw new FileUploadException("Failed to generate upload URL", e);
        }
    }
    
    private String buildKey(String folder, UUID ownerId, String fileName) {
        String extension = getFileExtension(fileName);
        String uniqueName = UUID.randomUUID() + extension;
        return folder + "/" + ownerId + "/" + uniqueName;
    }
    
    private String buildPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    private void validateImageType(String fileType) {
        List<String> allowed = Arrays.asList(allowedImageTypes.split(","));
        if (!allowed.contains(fileType)) {
            throw new FileUploadException(
                    "Invalid image type: " + fileType + ". Allowed types: " + allowedImageTypes);
        }
    }
    
    private void validateFileType(String fileType) {
        List<String> allowed = Arrays.asList(allowedFileTypes.split(","));
        if (!allowed.contains(fileType)) {
            throw new FileUploadException(
                    "Invalid file type: " + fileType + ". Allowed types: " + allowedFileTypes);
        }
    }
    
    /**
     * Result record for pre-signed URL generation.
     */
    public record PresignedUploadResult(String uploadUrl, String key, int expiresInMinutes) {
    }
}
