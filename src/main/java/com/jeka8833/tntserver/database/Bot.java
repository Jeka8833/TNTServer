package com.jeka8833.tntserver.database;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bot extends User {
    private final Set<String> privileges = new HashSet<>();
    private boolean isAuthorised;

    public Bot(@NotNull UUID botUUID) {
        super(botUUID);
    }

    @Override
    public boolean isInactive() {
        return !isAuthorised;
    }

    @Override
    public void disconnect() {
        isAuthorised = false;

        privileges.clear();
        PlayersDatabase.deleteUser(uuid);
    }

    public void setLoginSuccessful() {
        isAuthorised = true;
    }

    public void addPrivileges(Collection<String> privileges) {
        this.privileges.clear();
        this.privileges.addAll(privileges);
    }

    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }
}
