package com.mac.orion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "FILE")
public class FileEntity {

  @Id
  @GeneratedValue
  private Long id;
  @Column(name = "HASH")
  private String hash;
  @Column(name = "NAME")
  private String name;
  @Column(name = "PATH")
  private String path;
  @Column(name = "SIZE")
  private Long size;
  @Column(name = "STATUS")
  private Status status;

}
