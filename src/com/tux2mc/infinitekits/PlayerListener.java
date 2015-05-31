package com.tux2mc.infinitekits;

import java.io.File;
import java.util.UUID;

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
		UUID playeruuid = event.getUniqueId();

		File playerFile = new File(plugin.serverConfig.getProperty("level-name") + "/playerdata/" + playeruuid.toString() + ".dat");

		if (!playerFile.exists()) {
			System.out.println(playerName + " is logging in for the first time.");
			plugin.prelogins.put(playeruuid, true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin ( PlayerJoinEvent event ) {
		String playerName = event.getPlayer().getName();
		UUID playeruuid = event.getPlayer().getUniqueId();

		if (plugin.prelogins.containsKey(playeruuid)) {
			plugin.prelogins.remove(playeruuid);
			if(plugin.kits.containsKey(plugin.firstloginkit.toLowerCase())) {
				KitSet kit = plugin.kits.get(plugin.firstloginkit.toLowerCase());
				Player player = event.getPlayer();
				ItemStack[] items = kit.getKitStacks();
				for(int i= 0; i < items.length; i++) {
					player.getInventory().addItem(items[i]);
				}
				if(!player.hasPermission("infinitekits.nocooldown")) {
					plugin.cooldowns.put(player.getUniqueId().toString() + "." + plugin.firstloginkit.toLowerCase(), new Long(System.currentTimeMillis()));
					if(plugin.kitwrite++ > 5) {
						plugin.saveCooldowns();
						plugin.kitwrite = 0;
					}
				}
			}
		}
	}
}
