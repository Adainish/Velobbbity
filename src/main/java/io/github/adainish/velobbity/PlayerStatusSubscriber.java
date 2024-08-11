package io.github.adainish.velobbity;

import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class PlayerStatusSubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        Velobbbity.instance.getLogger().info("Received message: " + message);
        // Parse the message to get the player ID and status
        String[] parts = message.split(" ");
        UUID playerId = UUID.fromString(parts[0]);
        String status = parts[1];

        // Check the status and redirect the player if necessary
        if (status.equals("safe")) {
            // Redirect the player
            //log redirect
            Velobbbity.instance.getLogger().info("Redirecting player " + playerId);
            Velobbbity.instance.redirect(playerId);
        }
    }
}
