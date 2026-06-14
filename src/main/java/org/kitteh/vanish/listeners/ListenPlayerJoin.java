/*
 * VanishNoPacket
 * Copyright (C) 2011-2022 Matt Baxter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.kitteh.vanish.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.metadata.LazyMetadataValue.CacheStrategy;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.vanish.VanishCheck;
import org.kitteh.vanish.VanishPerms;
import org.kitteh.vanish.VanishPlugin;

public final class ListenPlayerJoin implements Listener {
    private final VanishPlugin plugin;

    public ListenPlayerJoin(@NonNull VanishPlugin instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoinEarly(@NonNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.setMetadata("vanished", new LazyMetadataValue(this.plugin, CacheStrategy.NEVER_CACHE, new VanishCheck(this.plugin.getManager(), player.getName())));
        this.plugin.getManager().resetSeeing(player);

        boolean vanishedFromDB = false;
        if (this.plugin.getMySQLManager() != null) {
            vanishedFromDB = this.plugin.getMySQLManager().isVanished(player.getUniqueId());
            this.plugin.getMySQLManager().removePlayer(player.getUniqueId());
        }

        if (VanishPerms.joinVanished(player) || vanishedFromDB) {
            this.plugin.getManager().toggleVanishQuiet(player, false);
            this.plugin.hooksVanish(player);
        }
        this.plugin.hooksJoin(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoinLate(@NonNull PlayerJoinEvent event) {
        final StringBuilder statusUpdate = new StringBuilder();
        if (this.plugin.getManager().isVanished(event.getPlayer())) {
            String message = ChatColor.DARK_AQUA + "You have joined vanished.";
            if (VanishPerms.canVanish(event.getPlayer())) {
                message += " To appear: /vanish";
            }
            event.getPlayer().sendMessage(message);
            statusUpdate.append("vanished");
        }
        if (VanishPerms.joinWithoutAnnounce(event.getPlayer())) {
            this.plugin.getManager().getAnnounceManipulator().addToDelayedAnnounce(event.getPlayer().getName());
            if (this.plugin.isPaper()) {
                event.joinMessage(null);
            } else {
                //noinspection deprecation
                event.setJoinMessage(null);
            }
            if (statusUpdate.length() != 0) {
                statusUpdate.append(" and ");
            }
            statusUpdate.append("silently");
        }
        if (statusUpdate.length() != 0) {
            this.plugin.messageStatusUpdate(ChatColor.DARK_AQUA + event.getPlayer().getName() + " has joined " + statusUpdate.toString());
        }
    }
}
