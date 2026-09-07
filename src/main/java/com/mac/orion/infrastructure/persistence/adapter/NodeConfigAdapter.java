package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.domain.share.Constants;
import com.mac.orion.infrastructure.persistence.entity.NodeConfigEntity;
import com.mac.orion.infrastructure.persistence.repository.NodeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NodeConfigAdapter implements NodeConfigUseCase {

  private static final int CONFIG_ROW_ID = 1;

  private final NodeConfigRepository nodeConfigRepository;

  @Override
  public Integer getPort() {
    return getNodeConfig().getPort();
  }

  @Override
  public void updatePort(int port) {
    final NodeConfigEntity row = getNodeConfig();
    row.setPort(port);
    nodeConfigRepository.save(row);
    log.info("NODE_CONFIG updated: port={}", port);
  }

  @Override
  public Integer getLimitK() {
    final Integer limitK = getNodeConfig().getLimitK();
    return limitK == null ? Constants.LIMIT_K_DEFAULT : limitK; // defensive: null only on legacy rows
  }

  @Override
  public void updateLimitK(int limitK) {
    final NodeConfigEntity row = getNodeConfig();
    row.setLimitK(limitK);
    nodeConfigRepository.save(row);
    log.info("NODE_CONFIG updated: limitK={}", limitK);
  }

  /**
   * Fixed row (id = 1). If it does not exist (double safety net after the SQL seed),
   * it is created with the default port.
   */
  private NodeConfigEntity getNodeConfig() {
    return nodeConfigRepository.findById(CONFIG_ROW_ID).orElseGet(() -> {
      final NodeConfigEntity row = new NodeConfigEntity();
      row.setId(CONFIG_ROW_ID);
      row.setPort(Constants.P2P_PORT_DEFAULT);
      row.setLimitK(Constants.LIMIT_K_DEFAULT);
      return nodeConfigRepository.save(row);
    });
  }

}
