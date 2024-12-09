package com.jeka8833.tntserver.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Entity
@Table(name = "tntclient_user_roles_and_keys")
public class TNTClientUserModel {
    @Id
    @Column(name = "user")
    private UUID user;

    @Nullable
    @Column(name = "roles")
    private String roles;

    @Nullable
    @Column(name = "static_key")
    private UUID staticKey;
}
