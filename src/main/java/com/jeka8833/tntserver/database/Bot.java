package com.jeka8833.tntserver.database;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Bot extends User {
    private final Set<String> privileges = ConcurrentHashMap.newKeySet();

    public Bot(@NotNull UUID botUUID) {
        super(botUUID);
    }

    @Override
    public boolean isInactive() {
        return privileges.isEmpty();
    }

    @Override
    public void disconnect() {
        privileges.clear();
        PlayersDatabase.deleteUser(uuid);
    }

    public void addPrivileges(Collection<String> privileges) {
        this.privileges.clear();
        this.privileges.addAll(privileges);
    }

    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }
}
