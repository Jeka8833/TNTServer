package com.jeka8833.tntserver.old.database.storage;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

public class TNTPlayerStorage {
    public static final byte STATUS_ONLINE = 3;
    public static final byte STATUS_AFK = 2;
    public static final byte STATUS_INVISIBLE = 1;
    public static final byte STATUS_OFFLINE = 0;

    public @Nullable String version;
    public long forceBlock;
    public byte donate;

    public @Nullable TNTPlayerPingStorage playerPing;
    public @Nullable String gameInfo;
    public long forceActive;
    public long activeModules;

    @MagicConstant(flags = {STATUS_ONLINE, STATUS_AFK, STATUS_INVISIBLE, STATUS_OFFLINE})
    public byte status;
    public int fight;
}
