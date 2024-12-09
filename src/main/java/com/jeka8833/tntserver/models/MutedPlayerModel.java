package com.jeka8833.tntserver.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

@Entity
@Table(name = "tntclient_muted_player")
public class MutedPlayerModel {
    @Id
    @Column(name = "player")
    private UUID player;

    @Column(name = "discord_moderator_id")
    private long moderator;

    @Nullable
    @Column(name = "reason")
    private String reason;

    @Column(name = "unmute_time")
    private Instant unmuteTime;
}
