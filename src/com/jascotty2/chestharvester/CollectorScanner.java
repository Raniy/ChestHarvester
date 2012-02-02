/**
 * Copyright (C) 2011 Jacob Scott <jascottytechie@gmail.com>
 * Description: thread to scan for chest collection
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
package com.jascotty2.chestharvester;

import me.jascotty2.libv01.bukkit.inventory.ChestManip;
import me.jascotty2.libv01.bukkit.inventory.ItemStackManip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import me.jascotty2.bettershop.BetterShop;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.ItemStack;

public class CollectorScanner implements Runnable {

    public long interval = 1000;
    public boolean autoStack = true;
    private int taskID = -1;
    ChestHarvester plugin;

    public CollectorScanner(ChestHarvester plugin) {
        this.plugin = plugin;
    } // end default constructor

    public void start() {
        autoStack = plugin.config.autoStack;
        start(interval);
    }

    public void start(long wait) {
        //(new Timer()).scheduleAtFixedRate(this, wait, wait);
        // 20 ticks per second
        this.interval = wait;
        taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 100, (wait * 20) / 1000);
    }

    public void cancel() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    @Override
    public void run() {
//        long st = System.currentTimeMillis();
        dropChestScan();
//        System.out.println("scan latency: " + (System.currentTimeMillis() - st));
    }

    /**
     * scans for drops, then puts them into the first chest in a 1-block radius that can hold them
     */
    public void dropChestScan() {
        try {
//            int mvd = 0;
            Map<Block, ItemStack[]> chests = new HashMap<Block, ItemStack[]>();
            for (World world : plugin.getServer().getWorlds()) {
                String w = world.getName().toLowerCase();
                if (plugin.config.disabledWorlds.contains(w)) {
                    continue;
                }
                for (Item item : world.getEntitiesByClass(Item.class)) {
                    for (Block block : getScanBlocks(item.getLocation().getBlock())) {
                        if (block.getType() == Material.CHEST) {
                            if (plugin.betterShopPlugin != null
                                    && BetterShop.getSettings().chestShopEnabled
                                    && BetterShop.getChestShop() != null
                                    && BetterShop.getChestShop().hasChestShop(block)) {
                                continue;
                            }
                            ItemStack is[] = chests.get(block);
                            if (is == null) {
                                is = ChestManip.getContents((Chest) block.getState());
                                chests.put(block, is);
                            }
//                            mvd += item.getItemStack().getAmount();
                            ItemStack no = ItemStackManip.add(is, item.getItemStack(), autoStack);
                            if (no.getAmount() == 0) {
                                item.remove();
                            } else {
                                item.setItemStack(no);
                                // mvd -= no.getAmount();
                            }
                            break;

                        }

                    }
                }
            }
            for (Map.Entry<Block, ItemStack[]> c : chests.entrySet()) {
                ChestManip.setContents((Chest) c.getKey().getState(), c.getValue());
            }

            if (plugin.config.storageCartsCollect) {
                Map<StorageMinecart, ItemStack[]> carts = new HashMap<StorageMinecart, ItemStack[]>();
                for (World world : plugin.getServer().getWorlds()) {
                    String w = world.getName().toLowerCase();
                    if (plugin.config.disabledWorlds.contains(w)) {
                        continue;
                    }
                    for (StorageMinecart ch : world.getEntitiesByClass(StorageMinecart.class)) {
                        for (Entity item : ch.getNearbyEntities(plugin.config.scanRange * 2, 
                                plugin.config.scanRange * 2, plugin.config.scanRange * 2)) {
                            if(item instanceof Item) {
                                ItemStack[] inv = carts.get(ch);
                                if(inv == null) {
                                    inv = ch.getInventory().getContents();
                                    carts.put(ch, inv);
                                }
//                               mvd += ((Item)item).getItemStack().getAmount();
                                ItemStack no = ItemStackManip.add(inv, ((Item)item).getItemStack(), autoStack);
                                if (no.getAmount() == 0) {
                                    item.remove();
                                } else {
                                    ((Item)item).setItemStack(no);
                                    // mvd -= no.getAmount();
                                }
                            }
                        }
                    }
                    for (Map.Entry<StorageMinecart, ItemStack[]> c : carts.entrySet()) {
                        c.getKey().getInventory().setContents(c.getValue());
                    }
                }
            }
//            System.out.println(mvd + " collected");
        } catch (Exception e) {
            ChestHarvester.Log(Level.SEVERE, e);
        }
    }

    public Block[] getScanBlocks(Block b) {
        if ((plugin.config.scanRange <= 1 || plugin.config.scanRange > 3)) {
            return plugin.config.allDirections
                    ? new Block[]{
                        b,
                        b.getRelative(BlockFace.DOWN),
                        b.getRelative(BlockFace.NORTH),
                        b.getRelative(BlockFace.EAST),
                        b.getRelative(BlockFace.SOUTH),
                        b.getRelative(BlockFace.WEST),
                        b.getRelative(BlockFace.UP),
                        b.getRelative(BlockFace.NORTH_WEST),
                        b.getRelative(BlockFace.NORTH_EAST),
                        b.getRelative(BlockFace.SOUTH_EAST),
                        b.getRelative(BlockFace.SOUTH_WEST)}
                    : new Block[]{
                        b,
                        b.getRelative(BlockFace.DOWN),
                        b.getRelative(BlockFace.NORTH),
                        b.getRelative(BlockFace.EAST),
                        b.getRelative(BlockFace.SOUTH),
                        b.getRelative(BlockFace.WEST)};
        } else {
            ArrayList<Block> blocks = new ArrayList<Block>();
            for (int x = -plugin.config.scanRange;
                    x <= plugin.config.scanRange; ++x) {
                for (int z = -plugin.config.scanRange;
                        z <= plugin.config.scanRange; ++z) {
                    for (int y = -plugin.config.scanRange;
                            y <= (plugin.config.allDirections ? plugin.config.scanRange : 0); ++y) {
                        Block ab = b.getRelative(x, y, z);
                        if (plugin.config.allDirections) {
                            blocks.add(ab);
                        } else if (b.getLocation().distance(ab.getLocation()) <= plugin.config.scanRange) {
                            blocks.add(ab);
                        }
                    }
                }
            }
            return blocks.toArray(new Block[0]);
        }
    }
} // end class CollectorScanner
