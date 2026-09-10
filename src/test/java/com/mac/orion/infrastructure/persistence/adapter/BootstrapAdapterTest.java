package com.mac.orion.infrastructure.persistence.adapter;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.persistence.entity.BootstrapEntity;
import com.mac.orion.infrastructure.persistence.repository.BootstrapRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BootstrapAdapterTest extends BaseUnitTest {

  @Mock
  private BootstrapRepository bootstrapRepository;
  @InjectMocks
  private BootstrapAdapter bootstrapAdapter;

  private BootstrapEntity entity(String ip, String port, String peerId) {
    final BootstrapEntity e = new BootstrapEntity();
    e.setId(1L);
    e.setIp(ip);
    e.setPort(port);
    e.setPeerId(peerId);
    return e;
  }

  @Test
  void findAll_mapsEntitiesToDomain() {
    when(bootstrapRepository.findAll())
        .thenReturn(List.of(entity("1.2.3.4", "5050", "QmA"), entity("5.6.7.8", "5051", "QmB")));

    final List<Bootstrap> result = bootstrapAdapter.findAll();

    assertEquals(2, result.size());
    assertEquals(new Bootstrap("1.2.3.4", "5050", "QmA"), result.get(0));
    assertEquals(new Bootstrap("5.6.7.8", "5051", "QmB"), result.get(1));
  }

  @Test
  void save_whenMissing_createsNewRow() {
    when(bootstrapRepository.findByPeerId("QmA")).thenReturn(Optional.empty());
    when(bootstrapRepository.save(any(BootstrapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    bootstrapAdapter.save(new Bootstrap("1.2.3.4", "5050", "QmA"));

    verify(bootstrapRepository).save(any(BootstrapEntity.class));
  }

  @Test
  void save_whenPresent_updatesExistingRow() {
    final BootstrapEntity existing = entity("1.2.3.4", "5050", "QmA");
    when(bootstrapRepository.findByPeerId("QmA")).thenReturn(Optional.of(existing));
    when(bootstrapRepository.save(any(BootstrapEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    bootstrapAdapter.save(new Bootstrap("9.9.9.9", "6000", "QmA"));

    assertEquals("9.9.9.9", existing.getIp());
    assertEquals("6000", existing.getPort());
    verify(bootstrapRepository).save(existing);
  }

  @Test
  void delete_removesByPeerId() {
    final BootstrapEntity existing = entity("1.2.3.4", "5050", "QmA");
    when(bootstrapRepository.findByPeerId("QmA")).thenReturn(Optional.of(existing));

    bootstrapAdapter.delete(new Bootstrap("1.2.3.4", "5050", "QmA"));

    verify(bootstrapRepository).delete(existing);
  }

  @Test
  void delete_whenMissing_doesNothing() {
    when(bootstrapRepository.findByPeerId("QmX")).thenReturn(Optional.empty());

    bootstrapAdapter.delete(new Bootstrap("1.2.3.4", "5050", "QmX"));

    verify(bootstrapRepository, never()).delete(any(BootstrapEntity.class));
  }

  @Test
  void exists_delegatesToRepository() {
    when(bootstrapRepository.existsByPeerId("QmA")).thenReturn(true);

    assertTrue(bootstrapAdapter.exists(new Bootstrap("1.2.3.4", "5050", "QmA")));
  }

}
