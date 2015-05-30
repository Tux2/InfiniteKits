package com.tux2mc.infinitekits;

import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
	
	InfiniteKits plugin;
	
	public PlayerListener(InfiniteKits plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		String playerName = event.getName();

		File playerFile = new File(plugin.serverConfig.getProperty("level-name") + "/players/" + playerName + ".dat");

		if (!playerFile.exists()) {
			System.out.println(playerName + " is logging in for the first time.");
			plugin.prelogins.put(playerName, true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin ( PlayerJoinEvent event ) {
		String playerName = event.getPlayer().getName();

		if (plugin.prelogins.containsKey(playerName)) {
			plugin.prelogins.remove(playerName);
			if(plugin.kits.containsKey(plugin.firstloginkit.toLowerCase())) {
				KitSet kit = plugin.kits.get(plugin.firstloginkit.toLowerCase());
				Player player = event.getPlayer();
				ItemStack[] items = kit.getKitStacks();
				for(int i= 0; i < items.length; i++) {
					player.getInventory().addItem(items[i]);
				}
				if(!player.hasPermission("infinitekits.nocooldown")) {
					plugin.cooldowns.put(player.getName().toLowerCase() + ".firstloginkit", new Long(System.currentTimeMillis()));
					if(plugin.kitwrite++ > 5) {
						plugin.saveCooldowns();
						plugin.kitwrite = 0;
					}
				}
			}
		}
	}
}
