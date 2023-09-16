package com.github.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ClientAbortException.class)
    public Result handleClientAbortException(ClientAbortException e) {
        log.warn("客户端终止了连接");
        return Result.error(400, "客户端终止了连接");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldErr = e.getBindingResult().getFieldError();
        return Result.error(422, fieldErr.getDefaultMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.error(422, "缺少" + e.getParameterName() + "参数");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result requestNotReadable(Exception e) {
        log.error("请求格式不对", e);
        return Result.error(400, "Bad Request");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result illegalArgumentException(IllegalArgumentException e) {
        log.error("无效的参数", e);
        return Result.error(422, e.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public Result apiException(ApiException e) {
        log.error("api exception", e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常:", e);
        return Result.error("系统异常");
    }

}
