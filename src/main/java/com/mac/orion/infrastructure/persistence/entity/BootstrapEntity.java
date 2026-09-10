package com.mac.orion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "BOOTSTRAP")
public class BootstrapEntity {

  // IDENTITY (rowid de SQLite): GenerationType.AUTO se resuelve a secuencia (bootstrap_seq, inexistente).
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "IP")
  private String ip;
  @Column(name = "PORT")
  private String port;
  @Column(name = "PEER_ID")
  private String peerId;

}
