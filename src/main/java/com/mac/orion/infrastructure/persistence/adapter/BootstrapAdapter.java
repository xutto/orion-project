package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.persistence.entity.BootstrapEntity;
import com.mac.orion.infrastructure.persistence.repository.BootstrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdapter implements BootstrapUseCase {

  private final BootstrapRepository bootstrapRepository;

  @Override
  public List<Bootstrap> findAll() {
    return bootstrapRepository.findAll().stream().map(this::toDomain).toList();
  }

  @Override
  @Transactional
  public void save(Bootstrap bootstrap) {
    bootstrapRepository.findByPeerId(bootstrap.id()).ifPresentOrElse(
        existing -> {
          existing.setIp(bootstrap.ip());
          existing.setPort(bootstrap.port());
          bootstrapRepository.save(existing);
          log.info("BOOTSTRAP updated: {}:{} (id={})", bootstrap.ip(), bootstrap.port(), bootstrap.id());
        },
        () -> {
          bootstrapRepository.save(toEntity(bootstrap));
          log.info("BOOTSTRAP created: {}:{} (id={})", bootstrap.ip(), bootstrap.port(), bootstrap.id());
        });
  }

  @Override
  @Transactional
  public void delete(Bootstrap bootstrap) {
    bootstrapRepository.findByPeerId(bootstrap.id()).ifPresent(entity -> {
      bootstrapRepository.delete(entity);
      log.info("BOOTSTRAP deleted: {}:{} (id={})", bootstrap.ip(), bootstrap.port(), bootstrap.id());
    });
  }

  @Override
  public boolean exists(Bootstrap bootstrap) {
    return bootstrapRepository.existsByPeerId(bootstrap.id());
  }

  private Bootstrap toDomain(BootstrapEntity entity) {
    return new Bootstrap(entity.getIp(), entity.getPort(), entity.getPeerId());
  }

  private BootstrapEntity toEntity(Bootstrap bootstrap) {
    final BootstrapEntity entity = new BootstrapEntity();
    entity.setIp(bootstrap.ip());
    entity.setPort(bootstrap.port());
    entity.setPeerId(bootstrap.id());
    return entity;
  }

}
