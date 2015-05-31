package com.tux2mc.infinitekits;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;

public class IKStack {
	
	Material itemid = Material.AIR;
	int damage = 0;
	int quantity = 0;
	Map<Enchantment, Integer> enchants = null;
	String[] pages = null;
	String author = null;
	String title = null;
	List<PotionEffect> potioneffects = null;
	Color armorcolor = null;
	String name = null;
	List<String> lore = null;
	String headname = null;
	int repaircost = 0;
	IKBanner bannerdata = null;

	public IKStack(ItemStack is) {
		itemid = is.getType();
		damage = is.getDurability();
		quantity = is.getAmount();
		enchants = is.getEnchantments();
		ItemMeta meta = is.getItemMeta();
		name = meta.getDisplayName();
		lore = meta.getLore();
		if(itemid == Material.BOOK_AND_QUILL || itemid == Material.WRITTEN_BOOK) {
			BookMeta bi = (BookMeta)meta;
			List<String> lpages = bi.getPages();
			pages = new String[lpages.size()];
			for(int i = 0; i < lpages.size(); i++) {
				pages[i] = lpages.get(i);
			}
			author = bi.getAuthor();
			title = bi.getTitle();
		}else if(meta instanceof LeatherArmorMeta) {
			armorcolor = ((LeatherArmorMeta) meta).getColor();
		}else if(meta instanceof PotionMeta) {
			potioneffects = ((PotionMeta) meta).getCustomEffects();
		}else if(meta instanceof SkullMeta) {
			headname = ((SkullMeta) meta).getOwner();
		}else if(meta instanceof BannerMeta) {
			BannerMeta bannermeta = (BannerMeta) meta;
			DyeColor basecolor = bannermeta.getBaseColor();
			List<Pattern> patterns = bannermeta.getPatterns();
			bannerdata = new IKBanner(basecolor, patterns);
		}
		if(meta instanceof Repairable) {
			repaircost = ((Repairable) meta).getRepairCost();
		}
	}
	
	public IKStack(Material id, int damage, int amount, Map<Enchantment, Integer> enchantments) {
		itemid = id;
		this.damage = damage;
		quantity = amount;
		enchants = enchantments;
	}
	
	public IKStack(Material id, int damage, int amount, Map<Enchantment, Integer> enchantments, String[] pages, String author, String title) {
		itemid = id;
		this.damage = damage;
		quantity = amount;
		enchants = enchantments;
		this.pages = pages;
		this.author = author;
		this.title = title;
	}
	
	public Material getID() {
		return itemid;
	}
	
	public int getDamage() {
		return damage;
	}
	
	public int getAmount() {
		return quantity;
	}
	
	public Map<Enchantment, Integer> getEnchantments() {
		return enchants;
	}
	
	public String[] getPages() {
		return pages;
	}

	public String getAuthor() {
		return author;
	}

	public String getTitle() {
		return title;
	}
	
	public List<PotionEffect> getPotionEffects() {
		return potioneffects;
	}

	public void setPotionEffects(List<PotionEffect> potioneffects) {
		this.potioneffects = potioneffects;
	}

	public Color getArmorColor() {
		return armorcolor;
	}

	public void setArmorColor(Color armorcolor) {
		this.armorcolor = armorcolor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getLore() {
		return lore;
	}

	public void setLore(List<String> lore) {
		this.lore = lore;
	}

	public String getHeadName() {
		return headname;
	}

	public void setHeadName(String headname) {
		this.headname = headname;
	}

	public int getRepairCost() {
		return repaircost;
	}

	public void setRepairCost(int repaircost) {
		this.repaircost = repaircost;
	}
	
	public void setBannerData(IKBanner banner) {
		bannerdata = banner;
	}
	
	public IKBanner getBannerData() {
		return bannerdata;
	}

	public ItemStack[] getItemStacks() {
		if(quantity <= 64) {
			ItemStack[] is = {new ItemStack(itemid, quantity, (short)damage)};
			is[0].addUnsafeEnchantments(enchants);
			ItemMeta meta = is[0].getItemMeta();
			meta.setDisplayName(name);
			meta.setLore(lore);
			//Set book data if appropriate
			if(pages != null) {
				BookMeta bi = (BookMeta)meta;
				bi.setPages(pages);
				bi.setAuthor(author);
				bi.setTitle(title);
			}else if(meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta) meta).setColor(armorcolor);
			}else if(meta instanceof PotionMeta) {
				if(potioneffects != null) {
					for(PotionEffect pe : potioneffects) {
						((PotionMeta) meta).addCustomEffect(pe, true);
					}
				}else {
					((PotionMeta) meta).clearCustomEffects();
				}
			}else if(meta instanceof SkullMeta) {
				((SkullMeta) meta).setOwner(headname);
			}else if(meta instanceof BannerMeta) {
				if(bannerdata != null) {
					BannerMeta bmeta = (BannerMeta) meta;
					bmeta.setBaseColor(bannerdata.getBaseColor());
					bmeta.setPatterns(bannerdata.getPatterns());
				}
			}
			if(meta instanceof Repairable) {
				((Repairable) meta).setRepairCost(repaircost);
			}
			is[0].setItemMeta(meta);
			return is;
		}else {
			ItemStack[] is = new ItemStack[getNumberOfStacks()];
			int tempquantity = quantity;
			for(int i =0; i < is.length; i++) {
				int stackquantity = tempquantity;
				if(stackquantity > 64) {
					stackquantity = 64;
				}
				is[i] = new ItemStack(itemid, stackquantity, (short)damage);
				is[i].addUnsafeEnchantments(enchants);
				ItemMeta meta = is[i].getItemMeta();
				meta.setDisplayName(name);
				meta.setLore(lore);
				//Set book data if appropriate
				if(pages != null) {
					BookMeta bi = (BookMeta)meta;
					bi.setPages(pages);
					bi.setAuthor(author);
					bi.setTitle(title);
				}else if(meta instanceof LeatherArmorMeta) {
					((LeatherArmorMeta) meta).setColor(armorcolor);
				}else if(meta instanceof PotionMeta) {
					if(potioneffects != null) {
						for(PotionEffect pe : potioneffects) {
							((PotionMeta) meta).addCustomEffect(pe, true);
						}
					}else {
						((PotionMeta) meta).clearCustomEffects();
					}
				}else if(meta instanceof SkullMeta) {
					((SkullMeta) meta).setOwner(headname);
				}
				if(meta instanceof Repairable) {
					((Repairable) meta).setRepairCost(repaircost);
				}
				is[i].setItemMeta(meta);
				tempquantity -= stackquantity;
			}
			return is;
		}
	}
	
	public int getNumberOfStacks() {
		int overflowstack = 0;
		if(quantity%64 > 0) {
			overflowstack = 1;
		}
		return (quantity/64) + overflowstack;
	}
}