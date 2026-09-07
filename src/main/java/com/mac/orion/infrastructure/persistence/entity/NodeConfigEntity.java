package com.mac.orion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "NODE_CONFIG")
public class NodeConfigEntity {

  @Id
  private Integer id;
  @Column(name = "PORT")
  private Integer port;
  @Column(name = "LIMIT_K")
  private Integer limitK;

}
