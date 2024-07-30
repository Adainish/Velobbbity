package io.github.adainish.velobbity;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.adainish.velobbity.configuration.Config;
import io.github.adainish.velobbity.data.LobbyServer;
import io.github.adainish.velobbity.configuration.GSON;
import io.github.adainish.velobbity.data.VelobbityPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import us.ajg0702.queue.api.AjQueueAPI;
import us.ajg0702.queue.api.events.PreQueueEvent;
import us.ajg0702.queue.api.players.AdaptedPlayer;
import us.ajg0702.queue.api.queues.QueueServer;
import us.ajg0702.queue.api.server.AdaptedServer;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ProxyServer server;
    public Logger getLogger() {
        return logger;
    }


    @Inject
    public Velobbbity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("Initialised Velobbities main class.");
    }

    public JsonElement toJSONElement(List<LobbyServer> lobbyServers) {
        return GSON.PRETTY_MAIN_GSON().toJsonTree(lobbyServers);
    }

    public List<LobbyServer> fromJSONElement(JsonElement jsonElement) {
        Type listType = new TypeToken<List<LobbyServer>>() {}.getType();
        return GSON.PRETTY_MAIN_GSON().fromJson(jsonElement, listType);
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

            if (!this.config.hasKey("configuration")) {
                List<LobbyServer> lobbyServers = new ArrayList<>();
                //set default values for example lobby servers and max players per server
                for (int i = 1; i < 4; i++) {
                    LobbyServer lobbyServer = new LobbyServer("lobby" + i, 100);
                    lobbyServers.add(lobbyServer);
                    this.configuredLobbyServers.put(lobbyServer.serverName, lobbyServer);
                }
                this.config.setSubConfigElement("configuration", "servers", toJSONElement(lobbyServers));
                this.config.addSubComment("configuration", "servers", "Server name and max players for lobby servers");
                logger.atLevel(Level.INFO).log("Database configuration file created with default values.");
            } else {
                //get json object from config file
                JsonElement configSection = this.config.get("configuration");
                JsonElement serversSection = configSection.getAsJsonObject().get("servers");
                List<LobbyServer> lobbyServers = fromJSONElement(serversSection);
                lobbyServers.forEach(lobbyServer -> {
                    logger.atLevel(Level.INFO).log("Loaded lobby server: " + lobbyServer.serverName + " with max players: " + lobbyServer.maxPlayers);
                    this.configuredLobbyServers.put(lobbyServer.serverName, lobbyServer);
                });
                logger.atLevel(Level.INFO).log("Loaded configuration file and servers.");
            }
        } catch (Exception e) {
            //log that the directory could not be created
            logger.atLevel(Level.ERROR).log(e.getMessage());
            canLoad = false;
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        logger = LoggerFactory.getLogger("Velobbbity");
        this.setupConfig();
        if (!canLoad) {
            logger.atLevel(Level.ERROR).log("Could not load Velobbity. Disabling plugin.");
            return;
        }
        this.ajQueueAPI = AjQueueAPI.getInstance();
        this.ajSubscriptionsRegistration();
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
        //check if a players last server is a lobby server and if their desired server is not a lobby server
        if (configuredLobbyServers.containsKey(player.lastServer) && !configuredLobbyServers.containsKey(player.desiredServer)) {
            //if so, redirect them to their desired server
            AdaptedPlayer adaptedPlayer = AjQueueAPI.getInstance().getPlatformMethods().getPlayer(event.getPlayer().getUniqueId());
            //schedule a 5 second delay to allow data syncs to complete
            server.getScheduler()
                    .buildTask(this, () -> {
                        AjQueueAPI.getInstance().getQueueManager().addToQueue(adaptedPlayer, player.desiredServer);
                    })
                    .delay(5, TimeUnit.SECONDS)
                    .schedule();
        }
    }

    @Subscribe
    public void proxyLogout(DisconnectEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        cachedUUIDMappedData.remove(uuid);
    }

    //handle ajqueue events
    public void ajSubscriptionsRegistration() {
        AjQueueAPI.getInstance().listen(PreQueueEvent.class, event -> {
            //check if the players server is a lobby server, if not, redirect them to a lobby server and cancel the event
            VelobbityPlayer player = cachedUUIDMappedData.get(event.getPlayer().getUniqueId());
            if (player == null) {
                player = new VelobbityPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
                cachedUUIDMappedData.put(event.getPlayer().getUniqueId(), player);
            }
            if (!configuredLobbyServers.containsKey(player.lastServer)) {
                event.setCancelled(true);
                //find first available server (that is also configured as a lobby server)
                QueueServer queueServer = getAndSendAvailableLobbyServer(event.getPlayer(), player, event.getTarget().getName());
                if (queueServer != null) {
                    event.getPlayer().sendActionBar(Component.text("Redirecting to lobby server...").style(Style.style(TextColor.color(0x00FF00))));
                    // if exists, redirect player and inform them why.
                    AdaptedServer adaptedServer = ajQueueAPI.getPlatformMethods().getServer(queueServer.getName());
                    event.getPlayer().connect(adaptedServer);
                } else //if no available server exists, send below message to player
                    event.getPlayer().sendActionBar(Component.text("No available lobby servers. Please try again later.").style(Style.style(TextColor.color(0xFF0000))));
            }


        });
    }

    //method to get first available lobby server to distribute player

    public QueueServer getAndSendAvailableLobbyServer(AdaptedPlayer player, VelobbityPlayer velobbityPlayer, String desiredServer) {
        QueueServer availableServer = null;
        ImmutableList<QueueServer> servers = ajQueueAPI.getQueueManager().getServers();
        for (int i = 0, serversSize = servers.size(); i < serversSize; i++) {
            QueueServer queueServer = servers.get(i);
            logger.atLevel(Level.INFO).log("Checking server: " + queueServer.getName());
            configuredLobbyServers.values().forEach(lobbyServer -> logger.atLevel(Level.INFO).log("Lobby server: " + lobbyServer.serverName));
            if (configuredLobbyServers.containsKey(queueServer.getName())) {
                //check if server is full
                if (queueServer.isOnline() && queueServer.isJoinable(player)) {
                    //adjust velobbity player data
                    velobbityPlayer.lastServer = player.getServerName();
                    velobbityPlayer.desiredServer = desiredServer;
                    this.cachedUUIDMappedData.put(player.getUniqueId(), velobbityPlayer);
                    availableServer = queueServer;
                    break;
                }
            }
        }
        return availableServer;
    }

}
