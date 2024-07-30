package io.github.adainish.velobbity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import io.github.adainish.velobbity.configuration.Config;
import io.github.adainish.velobbity.data.LobbyServer;
import io.github.adainish.velobbity.configuration.GSON;
import io.github.adainish.velobbity.data.VelobbityPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import us.ajg0702.queue.api.AjQueueAPI;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

@Plugin(
        id = "velobbity",
        name = "Velobbbity",
        version = BuildConstants.VERSION,
        description = "Adds a lobby system to velocity, to redirect a player to a (available) lobby server before redirecting them to the requested service ensuring data syncs have time to do their job. For any proxy server using AJQueue for Velocity.",
        url = "iverium.nl",
        authors = {"Winglet"}
)
public class Velobbbity {

    public HashMap<UUID, VelobbityPlayer> cachedUUIDMappedData = new HashMap<>();
    public HashMap<String, LobbyServer> configuredLobbyServers = new HashMap<>();
    public String directory = "config/Velobbity";

    @Inject
    private Logger logger;

    public static Velobbbity instance;
    public AjQueueAPI ajQueueAPI;
    public Config config;

    public boolean canLoad = true;

    public Logger getLogger() {
        return logger;
    }

    public void setupConfig()
    {
        File directoryFolder = new File(directory);
        if (!directoryFolder.exists()) {
            //log that the directory does not exist
            logger.atLevel(Level.INFO).log("Iverium Core configuration directory does not exist. Creating...");
            if (directoryFolder.mkdirs()) {
                logger.atLevel(Level.INFO).log("Created Iverium Core configuration directory.");
            } else {
                //log that the directory could not be created
                throw new RuntimeException("Could not create configuration directory for Cobble Sync.");
            }
        }
        try {
            File configFile = new File(directory, "config.json");
            if (!configFile.exists()) {
                //log that the database file does not exist
                logger.atLevel(Level.INFO).log("Database configuration file does not exist. Creating...");
                try {
                    if (configFile.createNewFile()) {
                        logger.atLevel(Level.INFO).log("Created database configuration file.");
                    } else {
                        //log that the database file could not be created
                        throw new RuntimeException("Could not create database configuration file.");
                    }
                } catch (Exception e) {
                    //log that the database file could not be created
                    throw new RuntimeException("Could not create database configuration file.", e);
                }
            }


            this.config = new Config(directory, "config", GSON.PRETTY_MAIN_GSON());

            if (!this.config.hasKey("servers")) {
                //set default values for example lobby servers and max players per server
                for (int i = 1; i < 4; i++) {
                    LobbyServer lobbyServer = new LobbyServer("lobby" + i, 100);
                    this.config.setSubConfigElement("configuration", "servers", lobbyServer.toJSONElement());
                    this.config.addSubComment("configuration", "servers", "Server name and max players for lobby server " + i);
                    this.configuredLobbyServers.put(lobbyServer.serverName, lobbyServer);
                }
                logger.atLevel(Level.INFO).log("Database configuration file created with default values.");
            } else {
                this.config.get("configuration").getAsJsonObject().get("servers").getAsJsonObject().entrySet().forEach(entry -> {
                    LobbyServer lobbyServer = new LobbyServer().fromJSONElement(entry.getValue());
                    this.configuredLobbyServers.put(lobbyServer.serverName, lobbyServer);
                });
            }
        } catch (Exception e) {
            //log that the directory could not be created
            logger.atLevel(Level.ERROR).log("Could not create configuration directory for Velobbity.");
            canLoad = false;
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        logger = LoggerFactory.getLogger("Velobbbity");
        this.setupConfig();
        if (!canLoad) {
            logger.atLevel(Level.ERROR).log("Could not load Velobbity.");
            return;
        }
        this.ajQueueAPI = AjQueueAPI.getInstance();
    }

    @Subscribe
    public void onLogin(PostLoginEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        if (cachedUUIDMappedData.containsKey(uuid))
            return;
        VelobbityPlayer player = new VelobbityPlayer(uuid, username);
        this.cachedUUIDMappedData.put(uuid, player);
    }

    @Subscribe
    public void serverJoin(ServerConnectedEvent event)
    {
        VelobbityPlayer player;
        UUID uuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        player = !cachedUUIDMappedData.containsKey(uuid) ? new VelobbityPlayer(uuid, username) : cachedUUIDMappedData.get(uuid);
        player.lastServer = event.getServer().getServerInfo().getName();
        cachedUUIDMappedData.put(uuid, player);
    }

    @Subscribe
    public void proxyLogout(DisconnectEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        cachedUUIDMappedData.remove(uuid);
    }

}
