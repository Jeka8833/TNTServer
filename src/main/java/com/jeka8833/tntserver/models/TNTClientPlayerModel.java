package com.jeka8833.tntserver.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tntclient_user_roles_and_keys")
public class TNTClientPlayerModel {
    @Id
    @Column(name = "user", nullable = false)
    private UUID user;

    @Nullable
    @Column(name = "version")
    private String version;

    @Column(name = "blockModules")
    private long blockModules;

    @Column(name = "donate")
    private byte donate;

    @Column(name = "timeLogin", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    public Instant timeLogin;

    @Column(name = "firstLogin", nullable = false, updatable = false, insertable = false)
    public Instant firstLogin;
}
