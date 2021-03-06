/**
 * Copyright (C) 2011 Jacob Scott <jascottytechie@gmail.com>
 * Description: provides methods for farming, using resources from a chest
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.chestharvester;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CHPlayerListener implements Listener {

    ChestHarvester plugin = null;

    public CHPlayerListener(ChestHarvester plugin) {
        this.plugin = plugin;
    } // end default constructor

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()
                && event.getAction() == Action.LEFT_CLICK_BLOCK
                && plugin.config.manualHarvest
                && event.getClickedBlock().getType() == Material.CHEST) {
			String w = event.getClickedBlock().getWorld().getName().toLowerCase();
			if(plugin.config.disabledWorlds.contains(w)){
				return;
			}
            if (!plugin.config.harvestPermission
                    || CHPermissions.permission(event.getPlayer(), "ChestHarvester.harvest")) {
                if (plugin.config.directionalHarvest) {
                    plugin.harvester.autoFarm(event.getPlayer(), (Chest) event.getClickedBlock().getState());
                } else {
                    plugin.harvester.autoFarm((Chest) event.getClickedBlock().getState());
                }
            }
        }
    }
} // end class CHPlayerListener

