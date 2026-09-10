package com.mac.orion.infrastructure.persistence.repository;

import com.mac.orion.infrastructure.persistence.entity.BootstrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BootstrapRepository extends JpaRepository<BootstrapEntity, Long> {

  Optional<BootstrapEntity> findByPeerId(String peerId);

  boolean existsByPeerId(String peerId);

}
