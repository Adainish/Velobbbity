package io.github.adainish.velobbity.data;

import com.google.gson.JsonElement;
import io.github.adainish.velobbity.configuration.GSON;

public class LobbyServer
{
    public String serverName;
    public int maxPlayers;
    public LobbyServer()
    {
        this.serverName = "lobby";
        this.maxPlayers = 100;
    }

    public LobbyServer(String serverName, int maxPlayers)
    {
        this.serverName = serverName;
        this.maxPlayers = maxPlayers;
    }

    public JsonElement toJSONElement()
    {
        return GSON.PRETTY_MAIN_GSON().toJsonTree(this);
    }

    public LobbyServer fromJSONElement(JsonElement jsonElement)
    {
        return GSON.PRETTY_MAIN_GSON().fromJson(jsonElement, LobbyServer.class);
    }
}
