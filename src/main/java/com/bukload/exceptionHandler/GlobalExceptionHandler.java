package com.bukload.exceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1) IllegalArgumentException
     * - travel1: NOT_FOUND(404)
     * - 기존 ai: BAD_REQUEST(400)
     * 👉 통합 방침: travel1 기준을 따름 (404)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", e.getMessage()
                ));
    }

    /**
     * 2) Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", "요청 검증 실패"
        ));
    }

    /**
     * 3) 기타 예외
     * - travel1: INTERNAL_ERROR
     * - ai: { error: "서버 오류", message: e.getMessage() }
     * 👉 통합 방침: travel1 메시지 + message 포함
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", e.getMessage()
                ));
    }
}
