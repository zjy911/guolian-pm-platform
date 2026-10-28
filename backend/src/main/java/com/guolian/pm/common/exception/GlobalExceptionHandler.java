package com.guolian.pm.common.exception;

import com.guolian.pm.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。对外只暴露规范化错误结构，堆栈只落日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex, HttpServletRequest request) {
        log.warn("业务异常 uri={} code={} msg={}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), msg.isEmpty() ? "请求参数不合法" : msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBind(BindException ex) {
        String msg = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), msg.isEmpty() ? "请求参数不合法" : msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleParam(Exception ex) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), "请求参数不合法：" + ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), "不支持的请求方法");
    }

    /**
     * 路径不存在。Spring 6 对未匹配的静态资源请求抛 NoResourceFoundException，
     * 若不单独处理会落到兜底 500，把「接口写错」伪装成「服务故障」。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("接口不存在 uri={}", request.getRequestURI());
        return ApiResponse.fail(ErrorCode.NOT_FOUND.getCode(), "接口不存在：" + request.getRequestURI());
    }

    /**
     * 请求体无法解析（JSON 格式错误、字段类型不匹配）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式不正确，请检查字段类型");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception ex, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), ex);
        return ApiResponse.fail(ErrorCode.SERVER_ERROR.getCode(), "服务器开小差了，请稍后重试");
    }
}
