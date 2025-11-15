package com.bukload.review.api;

import com.bukload.auth.user.User;
import com.bukload.review.service.ReviewService;
import com.bukload.review.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Validated
public class UploadController {

    private final S3Uploader s3Uploader;
    private final ReviewService reviewService;

    /**
     * form-data:
     * courseSegmentId : long (어느 세그먼트에 리뷰 남길지)
     * images          : [file, file, ...] (0~10장)
     * textFile        : file (.txt 1개)   - 선택
     * text            : string(본문)      - 선택
     */
    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadReview(
            // 임시: 유저 ID는 헤더로 주입
            @AuthenticationPrincipal User user,   // 🔥 여기서 로그인된 유저 자동 주입

            @RequestParam(name = "courseSegmentId") Long courseSegmentId,

            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @RequestPart(name = "textFile", required = false) MultipartFile textFile,
            @RequestParam(name = "text", required = false) String text
    ) {

        // -------------------------------
        // (1) 이미지 업로드 처리
        // -------------------------------
        List<UploadResponse.FileItem> uploadedImages = new ArrayList<>();

        if (images != null) {
            if (images.size() > 10) {
                throw new IllegalArgumentException("이미지는 최대 10장까지 업로드 가능합니다.");
            }

            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    var uf = s3Uploader.upload(img, "reviews/images");
                    uploadedImages.add(UploadResponse.FileItem.from(uf));
                }
            }
        }

        // -------------------------------
        // (2) 텍스트 처리 (textFile 또는 text)
        // -------------------------------
        UploadResponse.FileItem textItem = null;

        String finalContent = null;
        String finalS3Key = null;
        String finalContentType = null;
        Long finalSizeBytes = null;

        // textFile 우선
        if (textFile != null && !textFile.isEmpty()) {

            var uf = s3Uploader.upload(textFile, "reviews/texts");
            textItem = UploadResponse.FileItem.from(uf);

            try {
                finalContent = new String(textFile.getBytes(), StandardCharsets.UTF_8);
                finalSizeBytes = (long) finalContent.getBytes(StandardCharsets.UTF_8).length;
                finalS3Key = uf.getKey();
                finalContentType = uf.getContentType();
            } catch (Exception e) {
                throw new RuntimeException("텍스트 파일 읽기 실패: " + e.getMessage(), e);
            }

        }
        // text 문자열 방식
        else if (text != null && !text.isBlank()) {

            var uf = s3Uploader.uploadText(text, "reviews/texts", "review.txt");
            textItem = UploadResponse.FileItem.from(uf);

            finalContent = text;
            finalSizeBytes = (long) text.getBytes(StandardCharsets.UTF_8).length;
            finalS3Key = uf.getKey();
            finalContentType = "text/plain; charset=UTF-8";
        }

        if (uploadedImages.isEmpty() && textItem == null) {
            throw new IllegalArgumentException("최소 1개의 파일(images) 또는 텍스트(text/textFile)를 업로드해야 합니다.");
        }

        // -------------------------------
        // (3) Review 엔티티로 저장
        // -------------------------------
        if (textItem != null) {
            reviewService.saveReview(
                    user.getId(),
                    courseSegmentId,
                    finalContent,
                    finalS3Key,
                    finalContentType,
                    finalSizeBytes
            );
        }

        // -------------------------------
        // (4) 응답 반환
        // -------------------------------
        return new UploadResponse(uploadedImages, textItem);
    }
}
