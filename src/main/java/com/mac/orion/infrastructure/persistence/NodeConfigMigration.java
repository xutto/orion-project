package com.mac.orion.infrastructure.persistence;

import com.mac.orion.domain.share.Constants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * One-shot, idempotent migration for NODE_CONFIG tables created before LIMIT_K existed
 * (the port feature). New databases already have the column via tables01.sql.
 * Must run before any read of NodeConfigEntity.limitK ("no such column" otherwise).
 * Raw JDBC (JdbcTemplate): the DDL must not run inside a Hibernate transaction
 * (@PostConstruct has none, and EntityManager.executeUpdate would require one).
 */
//@Component
@RequiredArgsConstructor
@Slf4j
public class NodeConfigMigration {

  private final JdbcTemplate jdbcTemplate;

  @PostConstruct
  void ensureLimitKColumn() {
    // NOCASE: the column may have been created lowercase (Hibernate naming strategy) while
    // pragma_table_info returns the stored name verbatim (case-sensitive string comparison).
    final Integer present = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pragma_table_info('NODE_CONFIG') WHERE name = 'LIMIT_K' COLLATE NOCASE",
        Integer.class);
    if (present == null || present == 0) {
      jdbcTemplate.execute(
          "ALTER TABLE NODE_CONFIG ADD COLUMN LIMIT_K INTEGER NOT NULL DEFAULT "
              + Constants.LIMIT_K_DEFAULT);
      log.info("NODE_CONFIG migration: column LIMIT_K added (default {})", Constants.LIMIT_K_DEFAULT);
    } else {
      log.debug("NODE_CONFIG already has LIMIT_K, no migration needed");
    }
  }
}
