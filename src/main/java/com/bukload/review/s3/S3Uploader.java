package com.bukload.review.s3;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;
    private final S3Properties s3Properties;

    public UploadedFile upload(MultipartFile file, String prefix) {
        try {
            String contentType = detectContentType(file);
            String key = buildKey(prefix, file.getOriginalFilename());

            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000")
                    .build();

            s3.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = buildAccessUrl(key);
            return UploadedFile.builder()
                    .key(key)
                    .url(url)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    private String detectContentType(MultipartFile f) {
        String ct = f.getContentType();
        if (ct == null) ct = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (ct.startsWith("image/") || ct.equals(MediaType.TEXT_PLAIN_VALUE)) return ct;

        String name = f.getOriginalFilename() != null ? f.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".txt")) return MediaType.TEXT_PLAIN_VALUE;
        if (name.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp|svg)$")) return "image/*";

        throw new IllegalArgumentException("허용되지 않은 파일 타입: " + ct + " (" + name + ")");
    }

    private String buildKey(String prefix, String originalName) {
        String safeName = (originalName == null ? "file" : originalName)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String today = LocalDate.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s/%s_%s", prefix, today, uuid, safeName);
    }

    private String buildAccessUrl(String key) {
        if (!s3Properties.getPublicBaseUrl().isBlank()) {
            String encKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
            return s3Properties.getPublicBaseUrl() + "/" + encKey;
        }

        if (s3Properties.getPresignedGetSeconds() > 0) {
            var presigner = software.amazon.awssdk.services.s3.presigner.S3Presigner.create();
            var getReq = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();
            var presign = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofSeconds(s3Properties.getPresignedGetSeconds()))
                    .getObjectRequest(getReq)
                    .build();
            return presigner.presignGetObject(presign).url().toString();
        }

        return "https://s3.amazonaws.com/" + s3Properties.getBucket() + "/" + key;
    }

    public UploadedFile uploadText(String content, String prefix, String filenameHint) {
        try {
            String safeName = (filenameHint == null || filenameHint.isBlank()) ? "review.txt" : filenameHint;
            if (!safeName.toLowerCase().endsWith(".txt")) safeName += ".txt";

            String key = buildKey(prefix, safeName);

            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType("text/plain; charset=UTF-8")
                    .cacheControl("public, max-age=31536000")
                    .build();

            s3.putObject(putReq, RequestBody.fromString(content, StandardCharsets.UTF_8));

            String url = buildAccessUrl(key);
            return UploadedFile.builder()
                    .key(key)
                    .url(url)
                    .contentType("text/plain; charset=UTF-8")
                    .size(content.getBytes(StandardCharsets.UTF_8).length)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("S3 텍스트 업로드 실패: " + e.getMessage(), e);
        }
    }

    @Data
    @Builder
    public static class UploadedFile {
        private String key;
        private String url;
        private String contentType;
        private long size;
    }
}
