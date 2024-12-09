package com.jeka8833.tntserver.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

@Entity
@Table(name = "discordbot_discord_minecraft_user_connection")
public class ConnectedPlayerModel {
    @Column(name = "discord")
    private long discord;

    @Id
    @Column(name = "minecraft")
    private UUID player;
}
