package com.data.collection.platform.common.response;

public class ApiResponse<T> {
  private boolean success;
  private String code;
  private String message;
  private T data;

  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.code = ResultCode.SUCCESS.getCode();
    response.message = ResultCode.SUCCESS.getMessage();
    response.data = data;
    return response;
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    ApiResponse<T> response = success(data);
    response.message = message;
    return response;
  }

  public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = false;
    response.code = resultCode.getCode();
    response.message = message;
    response.data = null;
    return response;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
