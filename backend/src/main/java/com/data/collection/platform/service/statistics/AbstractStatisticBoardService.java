package com.data.collection.platform.service.statistics;

import com.data.collection.platform.entity.statistics.StatisticBoardDefinition;
import com.data.collection.platform.entity.statistics.StatisticBoardResponse;
import com.data.collection.platform.entity.statistics.StatisticDetailRequest;
import com.data.collection.platform.entity.statistics.StatisticDetailResponse;
import java.util.Map;

public abstract class AbstractStatisticBoardService {
  public abstract String boardKey();

  protected abstract StatisticBoardDefinition buildDefinition();

  protected abstract StatisticBoardResponse doLoadBoard(Map<String, String> filters);

  protected abstract StatisticDetailResponse doLoadDetail(StatisticDetailRequest request);

  public StatisticBoardDefinition getDefinition() {
    return buildDefinition();
  }

  public StatisticBoardResponse loadBoard(Map<String, String> filters) {
    return doLoadBoard(filters == null ? Map.of() : filters);
  }

  public StatisticDetailResponse loadDetail(StatisticDetailRequest request) {
    return doLoadDetail(request);
  }
}
