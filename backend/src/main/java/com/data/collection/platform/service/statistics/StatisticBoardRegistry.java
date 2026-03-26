package com.data.collection.platform.service.statistics;

import com.data.collection.platform.common.exception.BizException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class StatisticBoardRegistry {
  private final Map<String, AbstractStatisticBoardService> boardServiceMap;

  public StatisticBoardRegistry(List<AbstractStatisticBoardService> services) {
    this.boardServiceMap = services.stream()
        .collect(java.util.stream.Collectors.toMap(AbstractStatisticBoardService::boardKey, Function.identity()));
  }

  public AbstractStatisticBoardService getRequired(String boardKey) {
    AbstractStatisticBoardService service = boardServiceMap.get(boardKey);
    if (service == null) {
      throw new BizException("统计表不存在: " + boardKey);
    }
    return service;
  }
}
