package com.tux2mc.infinitekits;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import pgDev.bukkit.CommandPoints.CommandPoints;
import pgDev.bukkit.CommandPoints.CommandPointsAPI;

/**
 * InfiniteKits for Bukkit
 *
 * @author Tux2
 */
public class InfiniteKits extends JavaPlugin {
	
	ConcurrentHashMap<String, KitSet> kits = new ConcurrentHashMap<String, KitSet>();
	ConcurrentHashMap<String, String> casedkitnames = new ConcurrentHashMap<String, String>();
	public Economy economy = null;
	CommandPointsAPI cpAPI = null;
	
	public FileConfiguration config;

    public InfiniteKits() {
        super();
    }

   

    public void onEnable() {
    	config = getConfig();
    	File configfile = new File(getDataFolder().toString() + "/config.yml");
    	if(!configfile.exists()) {
    		createConfig();
    	}else {
    	}
    	setupEconomy();
    	setupCommandPoints();
    	loadKits();

        // Register our events
        //PluginManager pm = getServer().getPluginManager();
       
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }
    
    private void loadKits() {
    	ConfigurationSection csection = config.getConfigurationSection("kits");
    	if(csection != null) {
    		Map<String, Object> groupsMap = csection.getValues(false);
            Set<String> kitnames = groupsMap.keySet();
            for(String kitname : kitnames) {
            	KitSet newkitset = new KitSet();
            	newkitset.setKitPrice(config.getDouble("kits." + kitname + ".icoprice", 0));
            	newkitset.setCpPrice(config.getInt("kits." + kitname + ".cpprice", 0));
            	newkitset.setNeedsBoth(config.getBoolean("kits." + kitname + ".needboth", true));
            	ConfigurationSection ksection = config.getConfigurationSection("kits." + kitname + ".items");
            	Set<String> itemnames = ksection.getKeys(false);
            	for(String itemname : itemnames) {
            		String fullitemid = config.getString("kits." + kitname + ".items." + itemname + ".item");
            		if(fullitemid != null) {
            			int itemid = 0;
            			int damage = 0;
            			if(fullitemid.contains("-")) {
            				String[] id = fullitemid.split("-");
            				try {
                				itemid = Integer.parseInt(id[0]);
                				damage = Integer.parseInt(id[1]);
            				}catch (Exception e) {
            					
            				}
            			}else {
            				try {
            					itemid = Integer.parseInt(fullitemid);
            				}catch (Exception e) {
            					
            				}
            			}
            			//Make sure we got a proper integer before we make an itemstack here!
            			if(itemid != 0) {
            				int quantity = config.getInt("kits." + kitname + ".items." + itemname + ".quantity", 1);
            				ConfigurationSection enchantssection = config.getConfigurationSection("kits." + kitname + ".items." + itemname + ".enchantments");
                            HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
            				if(enchantssection != null) {
                                Set<String>enchantNames = enchantssection.getKeys(false);
                                for(String enchantname : enchantNames) {
                                	Enchantment tempenchant = Enchantment.getByName(enchantname.toUpperCase());
                                	if(tempenchant != null) {
                                		try {
                                    		enchants.put(tempenchant, new Integer(config.getInt("kits." + kitname + ".items." + itemname + ".enchantments." + enchantname)));
                                		}catch (Exception e){
                                			System.out.println("There was an error adding enchantment \"" + enchantname + "\" in the item \"" + itemname + "\" in the kit \"" + kitname + "\" Make sure it's an acutal number.");
                                		}
                                	}else {
                                		System.out.println("There is no enchantment with the name \"" + enchantname + "\". Please check the list again.");
                                	}
                                }
            				}
                            //If there is a quantity of 0 we don't need to add it...
                            if(quantity > 0) {
                                IKStack stack = new IKStack(itemid, damage, quantity, enchants);
                                newkitset.addItem(stack);
                            }
            			}
            		}
            	}
            	//Make sure there's items in this kit before adding it...
            	if(newkitset.getKit().size() > 0) {
            		kits.put(kitname.toLowerCase(), newkitset);
            	}
        		casedkitnames.put(kitname.toLowerCase(), kitname);
            }
    	}
		
	}



	private void createConfig() {
		KitSet newkit = new KitSet();
		newkit.addItem(new ItemStack(278, 1));
		newkit.addItem(new ItemStack(35, 5, (short)5));
		newkit.setCpPrice(2);
		newkit.setKitPrice(40000);
		newkit.setNeedsBoth(false);
		writeKit("SampleKit", newkit);
		newkit = new KitSet();
		ItemStack is = new ItemStack(278, 1);
		is.addEnchantment(Enchantment.DIG_SPEED, 3);
		newkit.addItem(is);
		newkit.setCpPrice(3);
		newkit.setKitPrice(8000);
		newkit.setNeedsBoth(true);
		writeKit("EnchantedSampleKit", newkit);
	}
    
	public void onDisable() {
        System.out.println("InfiniteKits brought to you by InfiniteMC!");
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			if(label.equalsIgnoreCase("kit") || label.equalsIgnoreCase("ikit") || label.equalsIgnoreCase("ckit")) {
				if(args.length > 0) {
					if(player.hasPermission("infinitekits.kit." + args[0].toLowerCase()) || player.hasPermission("infinitekits.kit.*")) {
						if(kits.containsKey(args[0].toLowerCase())) {
							KitSet kit = kits.get(args[0].toLowerCase());
							//Make sure they have enough cp or money to pay for the kit...
							if(hasenough(label, kit, player)) {
								//Some players are stupid enough to try to get a kit with a full inventory...
								if(hasemptyslots(player)) {
									ItemStack[] items = kit.getKitStacks();
									for(int i= 0; i < items.length; i++) {
										player.getInventory().addItem(items[i]);
									}
									takemoney(label, kit, player);
									return true;
								}else {
									player.sendMessage(ChatColor.DARK_RED + "You don't have any empty slots!");
									return true;
								}
							}else {
								player.sendMessage(ChatColor.DARK_RED + "Whoops, you don't have enough command points and/or money to buy the kit!");
								return true;
							}
						}else {
							player.sendMessage(ChatColor.DARK_RED + "That kit name doesn't exist!");
							return true;
						}
					}
				}else {
					return false;
				}
			}else if(label.equalsIgnoreCase("mkit")) {
				if(!player.hasPermission("infinitekits.modifykit")) {
					return true;
				}
				if(args.length > 1) {
					if(args[0].equalsIgnoreCase("additem")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.addItem(player.getItemInHand());
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "Item added to kit " + args[1] + "!");
							return true;
						}else {
							KitSet thekit = new KitSet();
							thekit.addItem(player.getItemInHand());
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "Item added to a new kit " + args[1] + "!");
							kits.put(args[1].toLowerCase(), thekit);
							casedkitnames.put(args[1].toLowerCase(), args[1].toLowerCase());
							return true;
						}
					}else if(args[0].equalsIgnoreCase("removeitem")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.removeItem(player.getItemInHand());
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "Item removed from kit " + args[1] + "!");
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
						}
					}else if(args[0].equalsIgnoreCase("setprice")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							try {
								thekit.setKitPrice(Double.parseDouble(args[2]));
								writeKit(args[1].toLowerCase(), thekit);
								player.sendMessage(ChatColor.AQUA + "Price set to " + thekit.getKitPrice() + " for kit " + args[1] + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}else {
							try {
								KitSet thekit = new KitSet();
								thekit.setKitPrice(Double.parseDouble(args[2]));
								writeKit(args[1].toLowerCase(), thekit);
								player.sendMessage(ChatColor.AQUA + "Price set to " + thekit.getKitPrice() + " for the new kit " + args[1] + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}
					}else if(args[0].equalsIgnoreCase("setcp")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							try {
								thekit.setCpPrice(Integer.parseInt(args[2]));
								writeKit(args[1].toLowerCase(), thekit);
								player.sendMessage(ChatColor.AQUA + "CP Price set to " + thekit.getKitPrice() + " for kit " + args[1] + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}else {
							try {
								KitSet thekit = new KitSet();
								thekit.setCpPrice(Integer.parseInt(args[2]));
								writeKit(args[1].toLowerCase(), thekit);
								player.sendMessage(ChatColor.AQUA + "CP Price set to " + thekit.getKitPrice() + " for the new kit " + args[1] + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}
					}else if(args[0].equalsIgnoreCase("requireboth")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.setNeedsBoth(true);
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + args[1] + "\" now requires both money and command points to redeem.");
							return true;
						}else {
							KitSet thekit = new KitSet();
							thekit.setNeedsBoth(true);
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "The new kit \"" + args[1] + "\" now requires both money and command points to redeem.");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("requireone")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.setNeedsBoth(false);
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + args[1] + "\" now requires either money or command points to redeem.");
							return true;
						}else {
							KitSet thekit = new KitSet();
							thekit.setNeedsBoth(false);
							writeKit(args[1].toLowerCase(), thekit);
							player.sendMessage(ChatColor.AQUA + "The new kit \"" + args[1] + "\" now requires either money or command points to redeem.");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("remove")) {
						config.set("kits." + args[1].toLowerCase(), null);
						kits.remove(args[1].toLowerCase());
						player.sendMessage(ChatColor.AQUA + "The kit \"" + args[1] + "\" was removed.");
						saveConfig();
						return true;
					}
				}else {
					return false;
				}
			}
		}
		return true;
	}
    
    private void writeKit(String kitname, KitSet thekit) {
    	String realname = kitname;
    	//Make sure we don't make duplicate kits...
    	if(casedkitnames.containsKey(kitname.toLowerCase())) {
    		realname = casedkitnames.get(kitname.toLowerCase());
    	}
    	config.set("kits." + realname, null);
    	config.set("kits." + realname + ".icoprice", thekit.getKitPrice());
    	config.set("kits." + realname + ".cpprice", thekit.getCpPrice());
    	config.set("kits." + realname + ".needboth", thekit.needBothCPandIco());
    	LinkedList<IKStack> items = thekit.getKitItems();
    	for(int i = 0; i < items.size(); i++) {
    		IKStack is = items.get(i);
        	config.set("kits." + realname + ".items.item" + i + ".item", itemWriter(is));
        	config.set("kits." + realname + ".items.item" + i + ".quantity", is.getAmount());
        	Map<Enchantment, Integer> enchants = is.getEnchantments();
        	Set<Enchantment> enchantkeys = enchants.keySet();
        	for(Enchantment enchant : enchantkeys) {
            	config.set("kits." + realname + ".items.item" + i + ".enchantments." + enchant.getName(), enchants.get(enchant));
        	}
    	}
    	saveConfig();
		
	}
    
    private String itemWriter(IKStack is) {
    	return String.valueOf(is.getID()) + "-" + String.valueOf(is.getDamage());
    }

	private void takemoney(String label, KitSet kit, Player player) {
    	if(player.hasPermission("infinitekits.free")) {
			player.sendMessage(ChatColor.DARK_GREEN + "You just got a free kit!");
    		return;
    	}
		if(kit.getKitPrice() <= 0 && kit.getCpPrice() <= 0) {
			player.sendMessage(ChatColor.DARK_GREEN + "You just got a free kit!");
			return;
		}else if(cpAPI == null && economy == null) {
			player.sendMessage(ChatColor.DARK_GREEN + "You just got a free kit!");
			return;
		}else if(cpAPI != null && economy == null) {
			if(kit.getCpPrice() <= 0) {
				player.sendMessage(ChatColor.DARK_GREEN + "You just got a free kit!");
				return;
			}else {
				cpAPI.removePoints(player.getName(), kit.getCpPrice(), "Purchased a kit", this);
				player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getCpPrice() + " command points!");
				return;
			}
		}else if(cpAPI == null && economy != null) {
			if(kit.getKitPrice() <= 0) {
				return;
			}else {
				economy.withdrawPlayer(player.getName(), kit.getKitPrice());
				player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getKitPrice() + " " + economy.currencyNamePlural());
				return;
			}
		}else if(cpAPI != null && economy != null) {
			//if the kit needs both points and money let's do this.
			if(kit.needBothCPandIco()) {
				cpAPI.removePoints(player.getName(), kit.getCpPrice(), "Purchased a kit", this);
				economy.withdrawPlayer(player.getName(), kit.getKitPrice());
				player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getKitPrice() + " " + economy.currencyNamePlural() + 
						" and " + kit.getCpPrice() + " command points!");
				return;
			//Otherwise we need to do these checks.
			}else {
				//If there are no command points price then it's a money only kit.
				if(kit.getCpPrice() <= 0) {
					economy.withdrawPlayer(player.getName(), kit.getKitPrice());
					player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getKitPrice() + " " + economy.currencyNamePlural());
					return;
				//If there is no price, then it's a command points only kit.
				}else if(kit.getKitPrice() <= 0) {
					cpAPI.removePoints(player.getName(), kit.getCpPrice(), "Purchased a kit", this);
					player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getCpPrice() + " command points!");
					return;
				//There are both command points prices and money prices at this point. Let's go deeper...
				/*With this command the user didn't specify whether they wanted to use cp or money,
				 * we will assume they want to use money, unless they are out, then we use
				 * command points...
				 */
				}else if(label.equalsIgnoreCase("kit")) {
					if(economy.getBalance(player.getName()) >= kit.getKitPrice()) {
						//They have enough money!
						economy.withdrawPlayer(player.getName(), kit.getKitPrice());
						player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getKitPrice() + " " + economy.currencyNamePlural());
						return;
					}else {
						//They didn't have enough money, so they must have enough command points! (we check prior, right?)
						cpAPI.removePoints(player.getName(), kit.getCpPrice(), "Purchased a kit", this);
						player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getCpPrice() + " command points!");
						return;
					}
				//They specified they want to spend money
				}else if(label.equalsIgnoreCase("ikit")) {
					economy.withdrawPlayer(player.getName(), kit.getKitPrice());
					player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getKitPrice() + " " + economy.currencyNamePlural());
					return;
				//they specified they just want to spend command points
				}else if(label.equalsIgnoreCase("ckit")) {
					cpAPI.removePoints(player.getName(), kit.getCpPrice(), "Purchased a kit", this);
					player.sendMessage(ChatColor.DARK_GREEN + "You just purchased a kit for " + kit.getCpPrice() + " command points!");
					return;
				}
			}
		}
	}



	private boolean hasemptyslots(Player player) {
    	if(player.getInventory().firstEmpty() == -1) {
    		return false;
    	}else {
    		return true;
    	}
    }
    
    private boolean hasenough(String label, KitSet kit, Player player) {
    	if(player.hasPermission("infinitekits.free")) {
    		return true;
    	}
		if(kit.getKitPrice() <= 0 && kit.getCpPrice() <= 0) {
			return true;
		}else if(cpAPI == null && economy == null) {
			//They don't have command points or economy enabled, free kits for everyone!
			return true;
		}else if(cpAPI != null && economy == null) {
			if(kit.getCpPrice() <= 0) {
				return true;
			}else if(cpAPI.hasPoints(player.getName(), kit.getCpPrice(), this)) {
				return true;
			}else {
				return false;
			}
		}else if(cpAPI == null && economy != null) {
			if(kit.getKitPrice() <= 0) {
				return true;
			}else if(economy.getBalance(player.getName()) >= kit.getKitPrice()) {
				return true;
			}else {
				return false;
			}
		}else if(cpAPI != null && economy != null) {
			//if the kit needs both points and money let's do this.
			if(kit.needBothCPandIco()) {
				if(cpAPI.hasPoints(player.getName(), kit.getCpPrice(), this) && economy.getBalance(player.getName()) >= kit.getKitPrice()) {
					return true;
				}else {
					return false;
				}
			//Otherwise we need to do these checks.
			}else {
				//If there are no command points price then it's a money only kit.
				if(kit.getCpPrice() <= 0) {
					if(economy.getBalance(player.getName()) >= kit.getKitPrice()) {
						return true;
					}else {
						return false;
					}
				//If there is no price, then it's a command points only kit.
				}else if(kit.getKitPrice() <= 0) {
					if(cpAPI.hasPoints(player.getName(), kit.getCpPrice(), this)) {
						return true;
					}else {
						return false;
					}
				//There are both command points prices and money prices at this point. Let's go deeper...
				/*With this command the user didn't specify whether they wanted to use cp or money,
				 * we will assume they want to use money, unless they are out, then we check for
				 * command points...
				 */
				}else if(label.equalsIgnoreCase("kit")) {
					if(economy.getBalance(player.getName()) >= kit.getKitPrice()) {
						//They have enough money!
						return true;
					}else if(cpAPI.hasPoints(player.getName(), kit.getCpPrice(), this)) {
						//They didn't have enough money, but they have enough command points!
						return true;
					}else {
						//If it gets to here they didn't have enough money or command points...
						return false;
					}
				//They specified they want to spend money, let's just check for that!
				}else if(label.equalsIgnoreCase("ikit")) {
					if(economy.getBalance(player.getName()) >= kit.getKitPrice()) {
						return true;
					}else {
						return false;
					}
				//they specified they just want to spend command points, let's just check for that!
				}else if(label.equalsIgnoreCase("ckit")) {
					if(cpAPI.hasPoints(player.getName(), kit.getCpPrice(), this)) {
						return true;
					}else {
						return false;
					}
				}
			}
		}
		//If it ever gets here there's a serious error...
		return false;
	}



	private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
    }
    
    private void setupCommandPoints() {
        Plugin commandPoints = this.getServer().getPluginManager().getPlugin("CommandPoints");
        if (commandPoints != null) {
            cpAPI = ((CommandPoints)commandPoints).getAPI();
        }
    }
}