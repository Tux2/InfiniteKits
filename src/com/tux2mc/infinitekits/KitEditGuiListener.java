package com.tux2mc.infinitekits;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitEditGuiListener implements Listener {
	
	InfiniteKits plugin;
	
	public KitEditGuiListener(InfiniteKits plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled=true)
	public void playerInventoryInteract(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if(plugin.editkits.containsKey(player.getUniqueId())) {
			if(!event.getView().getTitle().startsWith(plugin.kiteditidentifier)) {
				plugin.editkits.remove(player.getUniqueId());
				return;
			}
			EditKit ekit = plugin.editkits.get(player.getUniqueId());
			if(ekit.getEditType() == EditKit.EditType.NONE) {
				Inventory destInvent = event.getInventory();
				Integer slotClicked = event.getRawSlot();
				if( slotClicked < destInvent.getSize() ) {
					ItemStack item = event.getCurrentItem();
				    if(item != null) {
				    	ItemMeta meta = item.getItemMeta();
				    	if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_GREEN + "Save Changes")) {
				    		KitSet kit = plugin.kits.get(ekit.getName().toLowerCase());
				    		if(kit != null) {
				    			if(ekit.getKitItems().size() > 0) {
						    		kit.setItems(ekit.getKitItems());
						    		plugin.writeKit(kit);
						    		player.sendMessage(ChatColor.AQUA + "Kit " + ekit.getName() + " edited successfully!");
									plugin.editkits.remove(player.getUniqueId());
									player.closeInventory();
				    			}else {
									KitSet thekit = plugin.kits.remove(ekit.getName().toLowerCase());
									plugin.config.set("kits." + thekit.getName(), null);
									player.sendMessage(ChatColor.AQUA + "No items in kit \"" + thekit.getName() + "\", removing.");
									plugin.saveConfig();
									player.closeInventory();
				    			}
				    		}else {
				    			kit = new KitSet(ekit.getName(), ekit.getKitItems());
				    			plugin.kits.put(ekit.getName().toLowerCase(), kit);
				    			plugin.writeKit(kit);
					    		player.sendMessage(ChatColor.AQUA + "Kit " + ekit.getName() + " created successfully!");
								plugin.editkits.remove(player.getUniqueId());
								player.closeInventory();
				    		}
				    	}else if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_RED + "Cancel")) {
				    		player.sendMessage(ChatColor.RED + "Canceled editing kit, changes not saved.");
							plugin.editkits.remove(player.getUniqueId());
							player.closeInventory();
				    	}else {
				    		//Must be an item! Do the delete confirmation dialog
				    		ekit.setCurrrentStack(ekit.getKitItems().get(event.getRawSlot()));
				    		ekit.setEditType(EditKit.EditType.DELETE);
				    		plugin.displayEditGUI(player, ekit);
				    	}
				    }
				} else {
				    // slot clicked was in the player, not the remote container
					ItemStack item = event.getCurrentItem();
				    if(item != null) {
				    	//Must be an item! Do the add confirmation dialog
			    		ekit.setCurrrentStack(new IKStack(item));
			    		ekit.setEditType(EditKit.EditType.ADD);
			    		plugin.displayEditGUI(player, ekit);
				    }
				}
			}else if(ekit.getEditType() == EditKit.EditType.ADD) {
				ItemStack item = event.getCurrentItem();
			    if(item != null) {
			    	ItemMeta meta = item.getItemMeta();
			    	if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_GREEN + "Yes! Add Item to Kit")) {
			    		ekit.addItem(ekit.getCurrrentStack());
			    		ekit.setCurrrentStack(null);
			    		ekit.setEditType(EditKit.EditType.NONE);
			    		plugin.displayEditGUI(player, ekit);
			    	}else if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_RED + "No, Don't add item to Kit")) {
			    		ekit.setCurrrentStack(null);
			    		ekit.setEditType(EditKit.EditType.NONE);
			    		plugin.displayEditGUI(player, ekit);
			    	}
			    }
			}else if(ekit.getEditType() == EditKit.EditType.DELETE) {
				ItemStack item = event.getCurrentItem();
			    if(item != null) {
			    	ItemMeta meta = item.getItemMeta();
			    	if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_GREEN + "Yes! Delete item")) {
			    		ekit.removeItem(ekit.getCurrrentStack());
			    		ekit.setCurrrentStack(null);
			    		ekit.setEditType(EditKit.EditType.NONE);
			    		plugin.displayEditGUI(player, ekit);
			    	}else if(meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals(ChatColor.DARK_RED + "No, Don't delete item")) {
			    		ekit.setCurrrentStack(null);
			    		ekit.setEditType(EditKit.EditType.NONE);
			    		plugin.displayEditGUI(player, ekit);
			    	}
			    }
			}
			event.setResult(Event.Result.DENY);
			event.setCancelled(true);
		}
	}
}
