package com.mac.orion.application.out;

import com.mac.orion.domain.model.File;
import java.util.List;

public interface FilesPort {

  List<File> findAllPaginated(Integer size, Integer page);


  List<File> findAll();

  void saveAll(List<File> files);

  void save(File file);

  File findByHash(String hash);
}
