package com.teamtiger.review.api;

import com.teamtiger.review.db.ReviewTextJdbc;
import com.teamtiger.review.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    private final ReviewTextJdbc reviewTextJdbc;

    /**
     * form-data:
     * images   : [file, file, ...] (0~10장)
     * textFile : file (.txt 1개)   - 선택
     * text     : string(본문)      - 선택
     */
    @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadReview(
            // 임시: 헤더로 유저 ID 주입 (나중에 @AuthenticationPrincipal로 교체)
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") long userId,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @RequestPart(name = "textFile", required = false) MultipartFile textFile,
            @RequestParam(name = "text", required = false) String text
    ) {
        List<UploadResponse.FileItem> uploadedImages = new ArrayList<>();
        if (images != null) {
            if (images.size() > 10) throw new IllegalArgumentException("이미지는 최대 10장까지 업로드 가능합니다.");
            for (MultipartFile img : images) {
                if (img != null && !img.isEmpty()) {
                    var uf = s3Uploader.upload(img, "reviews/images");
                    uploadedImages.add(UploadResponse.FileItem.from(uf));
                }
            }
        }

        UploadResponse.FileItem textItem = null;

        // --- 텍스트 처리 ---
        if (textFile != null && !textFile.isEmpty()) {
            var uf = s3Uploader.upload(textFile, "reviews/texts");
            textItem = UploadResponse.FileItem.from(uf);

            try {
                String content = new String(textFile.getBytes(), StandardCharsets.UTF_8);
                long sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
                reviewTextJdbc.insert(userId, content, uf.getKey(), uf.getContentType(), sizeBytes);
            } catch (Exception e) {
                throw new RuntimeException("텍스트 파일 읽기 실패: " + e.getMessage(), e);
            }

        } else if (text != null && !text.isBlank()) {
            var uf = s3Uploader.uploadText(text, "reviews/texts", "review.txt");
            textItem = UploadResponse.FileItem.from(uf);

            long sizeBytes = text.getBytes(StandardCharsets.UTF_8).length;
            reviewTextJdbc.insert(userId, text, uf.getKey(), "text/plain; charset=UTF-8", sizeBytes);
        }

        if (uploadedImages.isEmpty() && textItem == null) {
            throw new IllegalArgumentException("최소 1개의 파일(images) 또는 텍스트(text/textFile)를 업로드해야 합니다.");
        }

        return new UploadResponse(uploadedImages, textItem);
    }
}
