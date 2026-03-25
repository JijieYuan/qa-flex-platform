package com.data.collection.platform.common.exception;

import com.data.collection.platform.common.response.ResultCode;

public class BizException extends RuntimeException {
  private final ResultCode resultCode;

  public BizException(String message) {
    this(ResultCode.BIZ_ERROR, message);
  }

  public BizException(ResultCode resultCode, String message) {
    super(message);
    this.resultCode = resultCode;
  }

  public ResultCode getResultCode() {
    return resultCode;
  }
}
