package com.jeka8833.tntserver.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "TC_Players")
public class TNTClientPlayerModel {
    @Id
    @Column(name = "user", nullable = false)
    private UUID user;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "blockModules", nullable = false)
    private long blockModules;

    @Column(name = "donate", nullable = false)
    private byte donate;
}
