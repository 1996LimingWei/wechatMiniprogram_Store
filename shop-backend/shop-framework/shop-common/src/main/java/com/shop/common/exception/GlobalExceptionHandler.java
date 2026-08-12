package com.shop.common.exception;

import com.shop.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServerException.class)
    public CommonResult<?> handleServerException(ServerException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        response.setStatus(toHttpStatus(e.getCode()));
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return CommonResult.error(ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public CommonResult<?> handleNoResourceFound(
            NoResourceFoundException e, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return CommonResult.error(HttpServletResponse.SC_NOT_FOUND, "接口不存在");
    }

    private int toHttpStatus(Integer code) {
        if (code == null) return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (code == 401 || code == 403 || code == 404 || code == 409 || code == 423 || code == 429) {
            return code;
        }
        if (code == 1404 || code == 1101) return HttpServletResponse.SC_NOT_FOUND;
        if (code == 1201) return HttpServletResponse.SC_CONFLICT;
        if (code >= 500 && code <= 599) return code;
        return HttpServletResponse.SC_BAD_REQUEST;
    }
}
