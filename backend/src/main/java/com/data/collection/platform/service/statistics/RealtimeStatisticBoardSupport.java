package com.data.collection.platform.service.statistics;

import com.data.collection.platform.entity.RealtimeWorkspaceStatusResponse;

public interface RealtimeStatisticBoardSupport {
  RealtimeWorkspaceStatusResponse getRealtimeStatus();

  RealtimeWorkspaceStatusResponse requestRealtimeRefresh();
}
