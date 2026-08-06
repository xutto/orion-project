package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.persistence.entity.FileEntity;
import com.mac.orion.infrastructure.persistence.repository.FilesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FilesPort {

  private final FilesRepository filesRepository;
  private final FilesMapper filesMapper;
//  private final RoutingTable routingDataHashTable;

  @Override
  public List<File> findAllPaginated(Integer size, Integer page) {

    final PageRequest pageRequest = PageRequest.of(page, size);

    return filesRepository.findAll(pageRequest)
        .map(filesMapper::toDomain)
        .toList();
  }

  @Override
  public List<File> findAll() {
//    final HostNode hostNodeData = routingDataHashTable.getHostNodeData(); // todo por que esta esto aqui?

    return filesRepository.findAll().stream().map(filesMapper::toDomain).toList();
  }

  @Override
  public void saveAll(List<File> files) {
    final List<FileEntity> filesList = files.stream().map(filesMapper::toEntity).toList();
    filesRepository.saveAll(filesList);
  }

  @Override
  public void save(File file) {
    final FileEntity fileEntity = filesMapper.toEntity(file);
    filesRepository.save(fileEntity);
  }

  @Override
  public File findByHash(String hash) {
    return filesRepository.getByHash(hash)
        .map(filesMapper::toDomain)
        .orElse(null);
  }
}
