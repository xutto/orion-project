package com.mac.orion.application.service;

import com.mac.orion.application.in.GroupingUpdateFilesUseCase;
import com.mac.orion.domain.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class GroupingUpdateFilesService implements GroupingUpdateFilesUseCase {


  @Override
  public Set<File> groupFilesBySize(Set<File> files) {
    return Set.of();
  }

  @Override
  public File updateFilePeersByLastSeenIfExisting(File fileParam, File fileLocal) {
    fileParam.getPeers().forEach((id, p) ->
        fileLocal.getPeers().compute(id, (id1, p1) -> {

              if (p1 == null) {
                return p;
              } else {

                if (!p1.getLastSeen().equals(p.getLastSeen())
                    && p.getLastSeen().isAfter(p1.getLastSeen())) {

                  return p1.toBuilder()
                      .id(id)
                      .lastSeen(p.getLastSeen())
                      .build();

                }
                return p1; // same time, not updated
              }
            }
        ));

    return fileLocal;
  }

  @Override
  public boolean updateNamesIfExisting(File fileParam, File fileLocal) {

    if (fileParam == null || fileParam.getNames() == null || fileParam.getNames().isEmpty()) {
      return false;
    }

    return fileLocal.getNames().addAll(fileParam.getNames());
  }




    /* todo, mejorar con mergeo de direccion, no se hace ahora porque requiere mantenimiento de peers y no sabemos donde hacerlo aun.
  private File updateFilePeersByLastSeenIfExisting(File fileParam, File fileLocal) {
    fileParam.getPeers().forEach((id, incomingPeer) ->
        fileLocal.getPeers().compute(id, (existingId, existingPeer) -> {
          if (existingPeer == null) {
            return incomingPeer;
          } else {
            var merged = existingPeer;

            // lastSeen más reciente
            if (incomingPeer.getLastSeen() != null
                && (existingPeer.getLastSeen() == null
                || incomingPeer.getLastSeen().isAfter(existingPeer.getLastSeen()))) {
              merged = merged.toBuilder().lastSeen(incomingPeer.getLastSeen()).build();
            }

            // unir direcciones
            if (incomingPeer.getAddress() != null && !incomingPeer.getAddress().isEmpty()) {
              var mergedAddresses = new HashSet<>(existingPeer.getAddress());
              mergedAddresses.addAll(incomingPeer.getAddress());
              merged = merged.toBuilder().address(mergedAddresses).build();
            }

            return merged;
          }
        })
    );
    return fileLocal;
  }
*/
}
