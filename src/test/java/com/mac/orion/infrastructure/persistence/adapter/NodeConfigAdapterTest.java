package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.domain.share.Constants;
import com.mac.orion.infrastructure.persistence.entity.NodeConfigEntity;
import com.mac.orion.infrastructure.persistence.repository.NodeConfigRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NodeConfigAdapterTest extends BaseUnitTest {

  @Mock
  private NodeConfigRepository nodeConfigRepository;
  @InjectMocks
  private NodeConfigAdapter nodeConfigAdapter;

  @Test
  void getLimitK_rowMissing_createsRowWithDefaults() {
    when(nodeConfigRepository.findById(1)).thenReturn(Optional.empty());
    when(nodeConfigRepository.save(any(NodeConfigEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    assertEquals(Constants.LIMIT_K_DEFAULT, nodeConfigAdapter.getLimitK());
    verify(nodeConfigRepository).save(any(NodeConfigEntity.class));
  }

  @Test
  void updateLimitK_persistsOnFixedRow() {
    final NodeConfigEntity row = new NodeConfigEntity();
    row.setId(1);
    row.setPort(Constants.P2P_PORT_DEFAULT);
    row.setLimitK(20);
    when(nodeConfigRepository.findById(1)).thenReturn(Optional.of(row));

    nodeConfigAdapter.updateLimitK(50);

    assertEquals(50, row.getLimitK());
    verify(nodeConfigRepository).save(row);
  }

  @Test
  void getLimitK_legacyRowNull_normalizedToDefault() {
    final NodeConfigEntity row = new NodeConfigEntity();
    row.setId(1);
    row.setPort(Constants.P2P_PORT_DEFAULT);
    // limitK null: legacy row
    when(nodeConfigRepository.findById(1)).thenReturn(Optional.of(row));

    assertEquals(Constants.LIMIT_K_DEFAULT, nodeConfigAdapter.getLimitK());
  }
}
