package com.data.collection.platform.common.exception;

import com.data.collection.platform.common.response.ApiResponse;
import com.data.collection.platform.common.response.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @ExceptionHandler(BizException.class)
  public ApiResponse<Void> handleBizException(BizException e) {
    log.warn("Business exception: {}", e.getMessage());
    return ApiResponse.fail(e.getResultCode(), e.getMessage());
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
  public ApiResponse<Void> handleBadRequest(Exception e) {
    log.warn("Bad request: {}", e.getMessage());
    return ApiResponse.fail(ResultCode.BAD_REQUEST, e.getMessage());
  }

  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ApiResponse.fail(ResultCode.SYSTEM_ERROR, "服务处理异常，请联系开发人员排查");
  }
}
