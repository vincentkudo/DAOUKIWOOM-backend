package com.daoukiwoom.roomflow.Exception;

import com.daoukiwoom.roomflow.DTO.Response.APIResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<APIResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        APIResponse<Void> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());
        apiResponse.setHttpStatus(errorCode.getHttpStatus());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException exception) {
        String enumKey = Objects.requireNonNull(exception.getFieldError()).getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException ignored) {
            errorCode = ErrorCode.VALIDATION_FAILED;
        }
        APIResponse<Void> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode == ErrorCode.VALIDATION_FAILED ? enumKey : errorCode.getMessage());
        apiResponse.setHttpStatus(errorCode.getHttpStatus());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<APIResponse<Void>> handleDenied() {
        return build(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<APIResponse<Void>> handleIntegrity(DataIntegrityViolationException exception) {
        String raw = String.valueOf(exception.getMostSpecificCause().getMessage());
        if (raw.contains("bookings_no_overlap")) {
            return build(ErrorCode.ROOM_ALREADY_BOOKED);
        }
        if (raw.contains("rooms_code_key") || raw.contains("code")) {
            return build(ErrorCode.ROOM_CODE_EXISTS);
        }
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<APIResponse<Void>> handlingException() {
        return build(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<APIResponse<Void>> build(ErrorCode errorCode) {
        APIResponse<Void> apiResponse = new APIResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        apiResponse.setHttpStatus(errorCode.getHttpStatus());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiResponse);
    }
}
