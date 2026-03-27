package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.mapper.GitlabSyncConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabConfigServiceTest {
  private GitlabSyncConfigMapper configMapper;
  private GitlabConfigService configService;

  @BeforeEach
  void setUp() {
    configMapper = mock(GitlabSyncConfigMapper.class);
    configService = new GitlabConfigService(configMapper);
  }

  @Test
  void shouldRejectCompensationIntervalBelowMinimum() {
    when(configMapper.selectOne(any())).thenReturn(null);

    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setCompensationIntervalMinutes(0);

    assertThatThrownBy(() -> configService.saveConfig(config))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("补偿间隔仅支持 1 到 720 分钟");

    verify(configMapper, never()).insert(any(GitlabSyncConfig.class));
  }

  @Test
  void shouldRejectCompensationIntervalAboveMaximum() {
    when(configMapper.selectOne(any())).thenReturn(null);

    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setCompensationIntervalMinutes(721);

    assertThatThrownBy(() -> configService.saveConfig(config))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("补偿间隔仅支持 1 到 720 分钟");

    verify(configMapper, never()).insert(any(GitlabSyncConfig.class));
  }
}
