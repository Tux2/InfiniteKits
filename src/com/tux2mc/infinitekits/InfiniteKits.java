package com.tux2mc.infinitekits;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.tux2mc.infinitekits.EditKit.EditType;

import pgDev.bukkit.CommandPoints.CommandPoints;
import pgDev.bukkit.CommandPoints.CommandPointsAPI;

/**
 * InfiniteKits for Bukkit
 *
 * @author Tux2
 */
public class InfiniteKits extends JavaPlugin {
	
	ConcurrentHashMap<String, KitSet> kits = new ConcurrentHashMap<String, KitSet>();
	
	ConcurrentHashMap<String, Boolean> prelogins = new ConcurrentHashMap<String, Boolean>();
	
	ConcurrentHashMap<UUID, EditKit> editkits = new ConcurrentHashMap<UUID, EditKit>();
	
	//The key is: playername.kitname (all lowercase) and the long is the last time it was used.
	ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<String, Long>();
	public Economy economy = null;
	CommandPointsAPI cpAPI = null;
	
	protected Properties serverConfig = new Properties();
	
	int kitwrite = 0;
	
	String firstloginkit = "firstloginkit";
	
	public FileConfiguration config;
	
	String kiteditidentifier = ChatColor.GOLD.toString() + ChatColor.RESET;

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
    	
    	try {
    		serverConfig.load(new BufferedReader(new FileReader("server.properties")));
    	} catch (FileNotFoundException e) {
    		System.out.println("[MultiSpawn] Couldn't find the server.properties file: " + e.getMessage());
    	} catch (IOException e) {
    		System.out.println("[MultiSpawn] Couldn't load the server.properties file: " + e.getMessage());
    	}
    	
    	setupEconomy();
    	setupCommandPoints();
    	loadKits();
    	loadCooldowns();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new KitEditGuiListener(this), this);
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }
    
    private void loadCooldowns() {

		// check for existing file
		File configFile = new File("plugins/InfiniteKits/cooldowns.ini");
		
		//if it exists, let's read it, if it doesn't, let's create it.
		if (configFile.exists()) {
			try {
				Properties cooldowndbs = new Properties();
				cooldowndbs.load(new FileInputStream(configFile));
				Set<Entry<Object, Object>> thecooldowns = cooldowndbs.entrySet();
				cooldowns.clear();
				for(Entry<Object, Object> cooldown : thecooldowns) {
					try {
						String[] thecooldownsplit = cooldown.getKey().toString().split("\\.");
						String kitname = thecooldownsplit[thecooldownsplit.length - 1];
						//Let's trim the database on import and delete lines for now non-existent kits
						if(kits.containsKey(kitname)) {
							cooldowns.put(cooldown.getKey().toString(), new Long(cooldown.getValue().toString()));
						}
					}catch (Exception e) {
						
					}
				}
			} catch (IOException e) {
				
			}
		}else {
		}
    }
    
    void saveCooldowns() {
    	try {
			BufferedWriter outChannel = new BufferedWriter(new FileWriter("plugins/InfiniteKits/cooldowns.ini"));
			
			outChannel.write("# Do not touch this file as it contains the last\n");
			outChannel.write("# time each kit was accessed by each player!\n");
			Set<Entry<String, Long>> scooldowns = cooldowns.entrySet();
			for(Entry<String, Long> thecooldown : scooldowns) {
				String[] thecooldownsplit = thecooldown.getKey().split("\\.");
				String kitname = thecooldownsplit[thecooldownsplit.length - 1];
				//Let's trim the database on export and delete lines for now non-existent kits
				if(kits.containsKey(kitname)) {
					outChannel.write(thecooldown.getKey() + " = " + thecooldown.getValue().toString() + "\n");
				}
			}
			outChannel.close();
		} catch (Exception e) {
			System.out.println("[InfiniteKits] - cooldown file creation failed, cooldowns will not persist across server reboots.");
			System.out.println(e);
		}
    }
    
    private void loadKits() {
    	if(config.getString("firstloginkit") == null) {
    		config.set("firstloginkit", firstloginkit);
    		saveConfig();
    	}
    	firstloginkit = config.getString("firstloginkit", firstloginkit);
    	ConfigurationSection csection = config.getConfigurationSection("kits");
    	if(csection != null) {
    		Map<String, Object> groupsMap = csection.getValues(false);
            Set<String> kitnames = groupsMap.keySet();
            for(String kitname : kitnames) {
            	KitSet newkitset = new KitSet(kitname);
            	newkitset.setKitPrice(config.getDouble("kits." + kitname + ".icoprice", 0));
            	newkitset.setCpPrice(config.getInt("kits." + kitname + ".cpprice", 0));
            	newkitset.setNeedsBoth(config.getBoolean("kits." + kitname + ".needboth", true));
            	newkitset.setCooldown(config.getInt("kits." + kitname + ".cooldown", 0));
            	ConfigurationSection ksection = config.getConfigurationSection("kits." + kitname + ".items");
            	Set<String> itemnames = ksection.getKeys(false);
            	for(String itemname : itemnames) {
            		String fullitemid = config.getString("kits." + kitname + ".items." + itemname + ".item");
            		if(fullitemid != null) {
            			Material itemid = Material.AIR;
            			int damage = 0;
            			if(fullitemid.contains("-")) {
            				String[] id = fullitemid.split("-");
            				try {
            					itemid = Material.getMaterial(id[0]);
            					if(itemid == null) {
                    				itemid = Material.getMaterial(Integer.parseInt(id[0]));
            					}
                				damage = Integer.parseInt(id[1]);
            				}catch (Exception e) {
            					
            				}
            			}else {
            				try {
            					itemid = Material.getMaterial(fullitemid);
            					if(itemid == null) {
                    				itemid = Material.getMaterial(Integer.parseInt(fullitemid));
            					}
            				}catch (Exception e) {
            					
            				}
            			}
            			//Make sure we got a proper integer before we make an itemstack here!
            			if(itemid != null && itemid != Material.AIR) {
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
            				String[] pages = null;
            				List<String> listpages = config.getStringList("kits." + kitname + ".items." + itemname + ".pages");
            				if(listpages != null && listpages.size() > 0) {
            					pages = new String[listpages.size()];
            					for(int i = 0; i < pages.length; i++) {
            						pages[i] = ChatColor.translateAlternateColorCodes('&', listpages.get(i));
            					}
            				}
            				String author = config.getString("kits." + kitname + ".items." + itemname + ".author", null);
            				String title = config.getString("kits." + kitname + ".items." + itemname + ".title", null);
            				if(title != null) {
            					title = ChatColor.translateAlternateColorCodes('&', title);
            				}
                            //If there is a quantity of 0 we don't need to add it...
                            if(quantity > 0) {
                                IKStack stack = new IKStack(itemid, damage, quantity, enchants, pages, author, title);
                                stack.setArmorColor((Color) config.get("kits." + kitname + ".items." + itemname + ".armorcolor", null));
                                stack.setHeadName(config.getString("kits." + kitname + ".items." + itemname + ".headname", null));
                                stack.setLore(convertFromAlternateColorCode(config.getStringList("kits." + kitname + ".items." + itemname + ".lore")));
                                String name = config.getString("kits." + kitname + ".items." + itemname + ".name", null);
                                if(name != null) {
                                	name = ChatColor.translateAlternateColorCodes('&', name);
                                }
                                stack.setName(name);
                                stack.setPotionEffects((List<PotionEffect>) config.getList("kits." + kitname + ".items." + itemname + ".potioneffects", null));
                                stack.setRepairCost(config.getInt("kits." + kitname + ".items." + itemname + ".repaircost", 0));
                                newkitset.addItem(stack);
                            }
            			}
            		}
            	}
            	//Make sure there's items in this kit before adding it...
            	if(newkitset.getKit().size() > 0) {
            		kits.put(kitname.toLowerCase(), newkitset);
                	Permission perm = new Permission("infinitekits.kit." +kitname.toLowerCase());
                	perm.addParent("infinitekits.kit.*", true);
            	}
            }
    	}
		
	}



	private void createConfig() {
		config.set("firstloginkit", firstloginkit);
		KitSet newkit = new KitSet("SampleKit");
		newkit.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
		newkit.addItem(new ItemStack(Material.WOOL, 5, (short)5));
		newkit.setCpPrice(2);
		newkit.setKitPrice(40000);
		newkit.setNeedsBoth(false);
		writeKit(newkit);
		newkit = new KitSet("EnchantedSampleKit");
		ItemStack is = new ItemStack(Material.DIAMOND_PICKAXE, 1);
		is.addEnchantment(Enchantment.DIG_SPEED, 3);
		newkit.addItem(is);
		newkit.setCpPrice(3);
		newkit.setKitPrice(8000);
		newkit.setNeedsBoth(true);
		writeKit(newkit);
	}
    
	public void onDisable() {
		saveCooldowns();
        System.out.println("InfiniteKits brought to you by InfiniteMC!");
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			if(label.equalsIgnoreCase("kit") || label.equalsIgnoreCase("ikit") || label.equalsIgnoreCase("ckit")) {
				if(args.length > 0) {
					if(player.hasPermission("infinitekits.kit." + args[0].toLowerCase())) {
						if(kits.containsKey(args[0].toLowerCase())) {
							KitSet kit = kits.get(args[0].toLowerCase());
							//Make sure they have enough cp or money to pay for the kit...
							if(hasenough(label, kit, player)) {
								if(!player.hasPermission("infinitekits.nocooldown")) {
									if(cooldowns.containsKey(player.getName().toLowerCase() + "." + args[0].toLowerCase())) {
										long lastused = cooldowns.get(player.getName().toLowerCase() + "." + args[0].toLowerCase());
										if(lastused + (kit.getCooldown() * 1000) > System.currentTimeMillis()) {
											player.sendMessage(ChatColor.DARK_RED + "You haven't waited long enough to use the kit again!");
											return true;
										}
									}
								}
								//Some players are stupid enough to try to get a kit with a full inventory...
								if(hasemptyslots(player)) {
									ItemStack[] items = kit.getKitStacks();
									for(int i= 0; i < items.length; i++) {
										player.getInventory().addItem(items[i]);
									}
									if(!player.hasPermission("infinitekits.nocooldown")) {
										cooldowns.put(player.getName().toLowerCase() + "." + args[0].toLowerCase(), new Long(System.currentTimeMillis()));
										if(kitwrite++ > 5) {
											saveCooldowns();
											kitwrite = 0;
										}
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
					player.sendMessage(ChatColor.BLUE + "Kits available: (color code: " + ChatColor.YELLOW + "both needed, " + ChatColor.DARK_PURPLE + "either or needed. " + ChatColor.BLUE + "CommandPoints, " + ChatColor.DARK_GREEN + "Money needed.)");
					Enumeration<KitSet> thekits = kits.elements();
					String allkits = "";
					while(thekits.hasMoreElements()) {
						KitSet kitname = thekits.nextElement();
						allkits = allkits + kitFormatter(kitname) + " | ";
					}
					player.sendMessage(allkits);
					return true;
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
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "Item added to kit " + thekit.getName() + "!");
							return true;
						}else {
							KitSet thekit = new KitSet(args[1]);
							thekit.addItem(player.getItemInHand());
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "Item added to a new kit " + args[1] + "!");
							kits.put(args[1].toLowerCase(), thekit);
							return true;
						}
					}else if(args[0].equalsIgnoreCase("removeitem")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.removeItem(player.getItemInHand());
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "Item removed from kit " + thekit.getName() + "!");
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("edit")) {
						EditKit ekit;
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							ekit = new EditKit(thekit, player.getUniqueId(), thekit.getName());
						}else {
							ekit = new EditKit(player.getUniqueId(), args[1]);
						}
						editkits.put(player.getUniqueId(), ekit);
						displayEditGUI(player, ekit);
						return true;
					}else if(args[0].equalsIgnoreCase("clone")) {
						if(args.length > 2) {
							if(kits.containsKey(args[1].toLowerCase())) {
								KitSet thekit = kits.get(args[1].toLowerCase());
								KitSet clonekit = kits.get(args[2].toLowerCase());
								if(clonekit != null) {
									player.sendMessage(ChatColor.DARK_RED + "You can't clone over an existing kit!");
									return true;
								}
								EditKit ekit = new EditKit(thekit, player.getUniqueId(), args[2]);
								editkits.put(player.getUniqueId(), ekit);
								displayEditGUI(player, ekit);
							}else {
								player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							}
						}else {
							player.sendMessage(ChatColor.DARK_RED + "You need to specify the name of the new kit.");
						}
						return true;
					}else if(args[0].equalsIgnoreCase("setprice")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							try {
								thekit.setKitPrice(Double.parseDouble(args[2]));
								writeKit(thekit);
								player.sendMessage(ChatColor.AQUA + "Price set to " + thekit.getKitPrice() + " for kit " + thekit.getName() + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
						}
					}else if(args[0].equalsIgnoreCase("setcp")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							try {
								thekit.setCpPrice(Integer.parseInt(args[2]));
								writeKit(thekit);
								player.sendMessage(ChatColor.AQUA + "CP Price set to " + thekit.getKitPrice() + " for kit " + thekit.getName() + "!");
								return true;
							}catch (Exception e) {
								return false;
							}
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("requireboth")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.setNeedsBoth(true);
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + thekit.getName() + "\" now requires both money and command points to redeem.");
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
						}
					}else if(args[0].equalsIgnoreCase("requireone")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.setNeedsBoth(false);
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + thekit.getName() + "\" now requires either money or command points to redeem.");
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("remove")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.remove(args[1].toLowerCase());
							config.set("kits." + thekit.getName(), null);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + thekit.getName() + "\" was removed.");
							saveConfig();
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							return true;
						}
					}else if(args[0].equalsIgnoreCase("setcooldown")) {
						if(kits.containsKey(args[1].toLowerCase())) {
							KitSet thekit = kits.get(args[1].toLowerCase());
							thekit.setCooldown(Long.valueOf(args[2]));
							writeKit(thekit);
							player.sendMessage(ChatColor.AQUA + "The kit \"" + thekit.getName() + "\" now has a cooldown of " + args[2] + " seconds.");
							return true;
						}else {
							player.sendMessage(ChatColor.DARK_RED + "Kit doesn't exist!");
							return true;
						}
					}
				}else {
					return false;
				}
			}else if(label.equalsIgnoreCase("newpotion")) {
				if(player.hasPermission("infinitekits.potions.create")) {
					if(args.length > 0) {
						int quantity = Integer.parseInt(args[0]);
						player.getInventory().addItem(new ItemStack(Material.POTION, quantity));
					}else {
						player.getInventory().addItem(new ItemStack(Material.POTION, 1));
					}
				}
			}else if(label.equalsIgnoreCase("npeffect")) {
				if(player.hasPermission("infinitekits.potions.create")) {
					if(args.length < 3) {
						player.sendMessage(ChatColor.GOLD + "/npeffect [potioneffect] [multiplier] [duration] <ambient>");
					}else if(player.getItemInHand() == null) {
						player.sendMessage(ChatColor.RED + "You must be holding an item!");
						return true;
					}else if(player.getItemInHand().getType() != Material.POTION) {
						player.sendMessage(ChatColor.GOLD + "Make sure to hold the potion you want to add the effect to!");
					}else {
						PotionMeta potionmeta = (PotionMeta) player.getItemInHand().getItemMeta();
						PotionEffectType effect = PotionEffectType.getByName(args[0]);
						if(effect == null) {
							player.sendMessage(ChatColor.DARK_RED + "Unknown potion effect. Known potion effects are:");
							StringBuilder potioneffects = new StringBuilder();
							for(PotionEffectType pet : PotionEffectType.values()) {
								if(pet != null) {
									potioneffects.append(pet.getName() + " ");
								}
							}
							player.sendMessage(ChatColor.DARK_GREEN + potioneffects.toString());
							return true;
						}
						int multiplier = Integer.parseInt(args[1]);
						int duration = Integer.parseInt(args[2]);
						if(args.length > 3) {
							boolean ambient = Boolean.parseBoolean(args[3]);
							PotionEffect pe = new PotionEffect(effect, duration, multiplier, ambient);
							if(potionmeta.addCustomEffect(pe, true)) {
								player.getItemInHand().setItemMeta(potionmeta);
								player.sendMessage(ChatColor.GOLD + "Potion effect applied successfully!");
							}
						}else {
							PotionEffect pe = new PotionEffect(effect, duration, multiplier);
							if(potionmeta.addCustomEffect(pe, true)) {
								player.getItemInHand().setItemMeta(potionmeta);
								player.sendMessage(ChatColor.GOLD + "Potion effect applied successfully!");
							}
						}
					}
				}
			}else if(label.equalsIgnoreCase("infinitekits")) {
				if(args.length > 0) {
					if(args[0].equalsIgnoreCase("setname")) {
						if(!player.hasPermission("infinitekits.customize.displayname")) {
							return true;
						}
						if(args.length > 1) {
							if(player.getItemInHand() == null) {
								player.sendMessage(ChatColor.RED + "You must be holding an item!");
								return true;
							}
							StringBuilder name = new StringBuilder();
							for(int i = 1; i < args.length; i++) {
								if(i > 1) {
									name.append(" ");
								}
								name.append(args[i]);
							}
							ItemMeta meta = player.getItemInHand().getItemMeta();
							meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name.toString()));
							player.getItemInHand().setItemMeta(meta);
							player.sendMessage(ChatColor.GOLD + "Name set to: " + ChatColor.translateAlternateColorCodes('&', name.toString()));
						}else {
							player.sendMessage(ChatColor.GOLD + "Usage: /infinitekits setname [name]");
						}
					}else if(args[0].equalsIgnoreCase("setlore")) {
						if(!player.hasPermission("infinitekits.customize.lore")) {
							return true;
						}
						if(args.length > 1) {
							if(player.getItemInHand() == null) {
								player.sendMessage(ChatColor.RED + "You must be holding an item!");
								return true;
							}
							StringBuilder lore = new StringBuilder();
							for(int i = 1; i < args.length; i++) {
								if(i > 1) {
									lore.append(" ");
								}
								lore.append(args[i]);
							}
							ItemMeta meta = player.getItemInHand().getItemMeta();
							LinkedList<String> llore = new LinkedList<String>();
							llore.add(ChatColor.translateAlternateColorCodes('&', lore.toString()));
							meta.setLore(llore);
							player.getItemInHand().setItemMeta(meta);
							player.sendMessage(ChatColor.GOLD + "Lore set to: " + ChatColor.translateAlternateColorCodes('&', lore.toString()));
						}else {
							player.sendMessage(ChatColor.GOLD + "Usage: /infinitekits setlore [new lore]");
						}
					}else if(args[0].equalsIgnoreCase("addlore")) {
						if(!player.hasPermission("infinitekits.customize.lore")) {
							return true;
						}
						if(args.length > 1) {
							if(player.getItemInHand() == null) {
								player.sendMessage(ChatColor.RED + "You must be holding an item!");
								return true;
							}
							StringBuilder lore = new StringBuilder();
							for(int i = 1; i < args.length; i++) {
								if(i > 1) {
									lore.append(" ");
								}
								lore.append(args[i]);
							}
							ItemMeta meta = player.getItemInHand().getItemMeta();
							LinkedList<String> llore;
							if(meta.getLore() != null) {
								llore = new LinkedList<String>(meta.getLore());
							}else {
								llore = new LinkedList<String>();
							}
							llore.add(lore.toString());
							meta.setLore(llore);
							player.getItemInHand().setItemMeta(meta);
							player.sendMessage(ChatColor.GOLD + "Added lore: " + lore.toString());
						}else {
							player.sendMessage(ChatColor.GOLD + "Usage: /infinitekits addlore [lore line]");
						}
					}else if(args[0].equalsIgnoreCase("setrepaircost")) {
						if(!player.hasPermission("infinitekits.customize.repaircost")) {
							return true;
						}
						if(args.length > 1) {
							if(player.getItemInHand() == null) {
								player.sendMessage(ChatColor.RED + "You must be holding an item!");
								return true;
							}
							ItemMeta meta = player.getItemInHand().getItemMeta();
							if(meta instanceof Repairable) {
								try {
									int repair = Integer.parseInt(args[1]);
									((Repairable) meta).setRepairCost(repair);
									player.getItemInHand().setItemMeta(meta);
									player.sendMessage(ChatColor.GOLD + "Repair cost set to: " + repair);
								}catch (NumberFormatException e) {
									player.sendMessage(ChatColor.RED + "You must specify the repair value as a whole number!");
								}
							}
						}else {
							player.sendMessage(ChatColor.GOLD + "Usage: /infinitekits setrepaircost [value]");
						}
					}else if(args[0].equalsIgnoreCase("playerhead")) {
						if(!player.hasPermission("infinitekits.customize.heads")) {
							return true;
						}
						if(args.length > 1) {
							ItemStack is = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
							SkullMeta meta = (SkullMeta) is.getItemMeta();
							meta.setOwner(args[1]);
							is.setItemMeta(meta);
							player.getInventory().addItem(is);
							player.sendMessage(ChatColor.GOLD + "Here's your head of " + args[1] + "!");
						}else {
							player.sendMessage(ChatColor.GOLD + "Usage: /infinitekits playerhead [playername]");
						}
					}
				}
			}
		}
		return true;
	}
    
    protected void writeKit(KitSet thekit) {
    	String realname = thekit.getName();
    	//Make sure we don't make duplicate kits...
    	if(kits.containsKey(realname.toLowerCase())) {
    		realname = kits.get(realname.toLowerCase()).getName();
    	}
    	config.set("kits." + realname, null);
    	config.set("kits." + realname + ".icoprice", thekit.getKitPrice());
    	config.set("kits." + realname + ".cpprice", thekit.getCpPrice());
    	config.set("kits." + realname + ".needboth", thekit.needBothCPandIco());
    	config.set("kits." + realname + ".cooldown", thekit.getCooldown());
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
        	// If it's a book, let's save the details.
        	if(is.getPages() != null) {
            	config.set("kits." + realname + ".items.item" + i + ".pages", convertToAlternateColorCode(is.getPages()));
            	config.set("kits." + realname + ".items.item" + i + ".author", is.getAuthor());
            	config.set("kits." + realname + ".items.item" + i + ".title", is.getTitle().replaceAll(String.valueOf(ChatColor.COLOR_CHAR), "&"));
        	}else {
            	config.set("kits." + realname + ".items.item" + i + ".armorcolor", is.getArmorColor());
            	config.set("kits." + realname + ".items.item" + i + ".headname", is.getHeadName());
            	config.set("kits." + realname + ".items.item" + i + ".lore", convertToAlternateColorCode(is.getLore()));
            	if(is.getName() != null) {
                	config.set("kits." + realname + ".items.item" + i + ".name", is.getName().replaceAll(String.valueOf(ChatColor.COLOR_CHAR), "&"));
            	}else {
                	config.set("kits." + realname + ".items.item" + i + ".name", is.getName());
            	}
            	config.set("kits." + realname + ".items.item" + i + ".potioneffects", is.getPotionEffects());
            	config.set("kits." + realname + ".items.item" + i + ".repaircost", is.getRepairCost());
        	}
    	}
    	saveConfig();
		
	}
    
    private List<String> convertToAlternateColorCode(List<String> source) {
    	if(source == null) {
    		return source;
    	}
    	LinkedList<String> converted = new LinkedList<String>();
    	for(String s : source) {
    		converted.add(s.replaceAll(String.valueOf(ChatColor.COLOR_CHAR), "&"));
    	}
    	return converted;
    }
    
    private List<String> convertToAlternateColorCode(String[] source) {
    	if(source == null) {
    		return null;
    	}
    	LinkedList<String> converted = new LinkedList<String>();
    	for(String s : source) {
    		converted.add(s.replaceAll(String.valueOf(ChatColor.COLOR_CHAR), "&"));
    	}
    	return converted;
    }
    
    private List<String> convertFromAlternateColorCode(List<String> source) {
    	if (source == null) {
    		return source;
    	}
    	LinkedList<String> converted = new LinkedList<String>();
    	for(String s : source) {
    		converted.add(ChatColor.translateAlternateColorCodes('&', s));
    	}
    	return converted;
    }
    
    private String itemWriter(IKStack is) {
    	return is.getID().name() + "-" + String.valueOf(is.getDamage());
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
    
    private String kitFormatter(KitSet thekit) {
    	String kit = "";
		if(thekit.needBothCPandIco()) {
			kit = kit + ChatColor.YELLOW;
		}else {
			kit = kit + ChatColor.DARK_PURPLE;
		}
		kit = kit + thekit.getName() + " ";
		if(thekit.getKitPrice() > 0) {
			kit = kit + ChatColor.DARK_GREEN + thekit.getKitPrice() + " ";
		}
		if(thekit.getCpPrice() > 0) {
			kit = kit + ChatColor.BLUE + thekit.getCpPrice() + " ";
		}
    	return kit;
    }
    
	@SuppressWarnings("unchecked")
	private void setupEconomy() {
         RegisteredServiceProvider economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = ((RegisteredServiceProvider<Economy>)economyProvider).getProvider();
        }
    }
    
    private void setupCommandPoints() {
        Plugin commandPoints = this.getServer().getPluginManager().getPlugin("CommandPoints");
        if (commandPoints != null) {
            cpAPI = ((CommandPoints)commandPoints).getAPI();
        }
    }
    
    public void displayEditGUI(Player player, EditKit kit) {
    	if(kit.getCurrrentStack() == null || kit.getEditType() == EditType.NONE) {
    		ItemStack[] items = new ItemStack[kit.getKitItems().size()];
    		for(int i = 0; i < items.length; i++) {
    			items[i] = kit.getKitItems().get(i).getItemStacks()[0];
    		}
        	int rows = items.length/9;
        	if(items.length%9 > 0) {
        		rows++;
        	}
        	String title = kiteditidentifier + ChatColor.GOLD + "Editing kit: " + kit.getName();
        	if(title.length() > 32) {
        		title = title.substring(0, 29) + "...";
        	}
    		Inventory inv = Bukkit.getServer().createInventory(null, 9*(rows + 1), title);
    		for(int i = 0; i < items.length; i++) {
    			inv.setItem(i, items[i]);
    		}
    		ItemStack savekit = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getWoolData());
    		ItemMeta savemeta = savekit.getItemMeta();
    		savemeta.setDisplayName(ChatColor.DARK_GREEN + "Save Changes");
    		savekit.setItemMeta(savemeta);
    		inv.setItem(rows*9 + 8, savekit);
    		ItemStack cancelkit = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
    		ItemMeta cancelmeta = cancelkit.getItemMeta();
    		cancelmeta.setDisplayName(ChatColor.DARK_RED + "Cancel");
    		cancelkit.setItemMeta(cancelmeta);
    		inv.setItem(rows*9 + 7, cancelkit);
    		player.openInventory(inv);
    	}else if(kit.getEditType() == EditType.ADD) {
    		Inventory inv = Bukkit.getServer().createInventory(null, 9, kiteditidentifier + ChatColor.DARK_GREEN + "Add Item");
    		ItemStack savekit = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getWoolData());
    		ItemMeta savemeta = savekit.getItemMeta();
    		savemeta.setDisplayName(ChatColor.DARK_GREEN + "Yes! Add Item to Kit");
    		savekit.setItemMeta(savemeta);
    		inv.setItem(0, savekit);
    		ItemStack cancelkit = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
    		ItemMeta cancelmeta = cancelkit.getItemMeta();
    		cancelmeta.setDisplayName(ChatColor.DARK_RED + "No, Don't add item to Kit");
    		cancelkit.setItemMeta(cancelmeta);
    		inv.setItem(8, cancelkit);
    		inv.setItem(4, kit.getCurrrentStack().getItemStacks()[0]);
    		player.openInventory(inv);
    	}else if(kit.getEditType() == EditType.DELETE) {
    		Inventory inv = Bukkit.getServer().createInventory(null, 9, kiteditidentifier + ChatColor.DARK_RED + "Delete Item");
    		ItemStack savekit = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getWoolData());
    		ItemMeta savemeta = savekit.getItemMeta();
    		savemeta.setDisplayName(ChatColor.DARK_GREEN + "Yes! Delete item");
    		savekit.setItemMeta(savemeta);
    		inv.setItem(0, savekit);
    		ItemStack cancelkit = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
    		ItemMeta cancelmeta = cancelkit.getItemMeta();
    		cancelmeta.setDisplayName(ChatColor.DARK_RED + "No, Don't delete item");
    		cancelkit.setItemMeta(cancelmeta);
    		inv.setItem(8, cancelkit);
    		inv.setItem(4, kit.getCurrrentStack().getItemStacks()[0]);
    		player.openInventory(inv);
    	}
    }
}