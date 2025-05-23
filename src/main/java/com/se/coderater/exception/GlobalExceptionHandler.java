package com.se.coderater.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice // 声明这是一个全局异常处理组件
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 处理 @Valid 注解校验失败抛出的 MethodArgumentNotValidException
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value()); // 通常是 400

        // 获取所有字段的校验错误信息
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

        body.put("errors", fieldErrors);
        body.put("message", "Validation failed"); // 或者更具体的错误摘要

        // logger.warn("Validation error: {}", fieldErrors); // 可以添加日志记录
        return new ResponseEntity<>(body, headers, status);
    }

    // 你还可以添加其他 @ExceptionHandler 方法来处理项目中可能出现的其他特定异常
    // 例如，处理登录时的 AuthenticationException (虽然 AuthController 中已经try-catch了)
    // @ExceptionHandler(AuthenticationException.class)
    // public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    //     Map<String, Object> body = new HashMap<>();
    //     body.put("timestamp", System.currentTimeMillis());
    //     body.put("status", HttpStatus.UNAUTHORIZED.value());
    //     body.put("error", "Unauthorized");
    //     body.put("message", ex.getMessage()); // 或者自定义消息 "Invalid credentials"
    //     return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    // }

    // 处理通用的 Exception (作为最后的防线)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception occurred", ex); // 记录严重错误
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please try again later.");
        // 在生产环境中，不应将 ex.getMessage() 直接暴露给客户端，除非它是安全的
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}