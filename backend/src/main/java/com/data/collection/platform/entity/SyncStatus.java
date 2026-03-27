package com.data.collection.platform.entity;

public enum SyncStatus {
  IDLE,
  PENDING,
  QUEUED,
  RUNNING,
  CANCELLING,
  CANCELLED,
  SUCCESS,
  FAILED,
  TIMEOUT
}
