package com.mac.orion.infrastructure.persistence.repository;

import com.mac.orion.infrastructure.persistence.entity.NodeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeConfigRepository extends JpaRepository<NodeConfigEntity, Integer> {
}
