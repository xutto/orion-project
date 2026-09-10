package com.mac.orion.application.out;

import com.mac.orion.domain.model.settings.Bootstrap;
import java.util.List;

/**
 * Port for the manually-managed bootstrap nodes persisted in the database (BOOTSTRAP table).
 * The database is the single source of truth for bootstraps (KAD gateways), consistent with
 * PORT and LimitK in NODE_CONFIG.
 * <p>
 * A bootstrap is a manually-entered peer, distinct from an ephemeral routing-table peer (which is
 * discovered by KAD, lives only in memory and is NEVER persisted here).
 */
public interface BootstrapUseCase {

  /**
   * @return all persisted bootstraps (empty list if none)
   */
  List<Bootstrap> findAll();

  /**
   * Persists a bootstrap, idempotent by peer id (UNIQUE(PEER_ID)): an existing row is updated,
   * a new one created.
   *
   * @param bootstrap the bootstrap to persist (validated by the caller before persisting)
   */
  void save(Bootstrap bootstrap);

  /**
   * Deletes a bootstrap by its peer id.
   *
   * @param bootstrap the bootstrap to delete (matched by peer id)
   */
  void delete(Bootstrap bootstrap);

  /**
   * @param bootstrap the bootstrap to check (matched by peer id)
   * @return true if a bootstrap with that peer id already exists
   */
  boolean exists(Bootstrap bootstrap);

}
