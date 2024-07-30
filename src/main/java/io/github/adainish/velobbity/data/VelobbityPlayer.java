package io.github.adainish.velobbity.data;

import java.util.UUID;

public class VelobbityPlayer
{
    public UUID uuid;
    public String username;
    public String lastServer;
    public String desiredServer;

    public VelobbityPlayer(UUID uuid)
    {
        this.uuid = uuid;
    }

    public VelobbityPlayer(UUID uuid, String username)
    {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid()
    {
        return this.uuid;
    }

    public String getUsername()
    {
        return this.username;
    }

}
