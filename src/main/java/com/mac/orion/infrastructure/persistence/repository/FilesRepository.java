package com.mac.orion.infrastructure.persistence.repository;

import com.mac.orion.infrastructure.persistence.entity.FileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FilesRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByPathAndSize(String path, Long size);

    Optional<FileEntity> getByHash(String hash);
}
