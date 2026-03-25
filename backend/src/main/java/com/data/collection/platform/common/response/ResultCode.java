package com.data.collection.platform.common.response;

public enum ResultCode {
  SUCCESS("00000", "成功"),
  BAD_REQUEST("A0400", "请求参数错误"),
  BIZ_ERROR("B0001", "业务处理失败"),
  SYSTEM_ERROR("C0001", "系统异常");

  private final String code;
  private final String message;

  ResultCode(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
