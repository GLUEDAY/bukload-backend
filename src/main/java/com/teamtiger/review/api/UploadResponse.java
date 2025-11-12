package com.teamtiger.review.api;

import com.teamtiger.review.s3.S3Uploader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UploadResponse {
    private List<FileItem> images;
    private FileItem text;

    @Data
    @AllArgsConstructor
    public static class FileItem {
        private String key;
        private String url;
        private String contentType;
        private long size;

        public static FileItem from(S3Uploader.UploadedFile uf) {
            return new FileItem(uf.getKey(), uf.getUrl(), uf.getContentType(), uf.getSize());
        }
    }
}
