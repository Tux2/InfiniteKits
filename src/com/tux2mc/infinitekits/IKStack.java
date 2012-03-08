package com.tux2mc.infinitekits;

import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class IKStack {
	
	int itemid = 0;
	int damage = 0;
	int quantity = 0;
	Map<Enchantment, Integer> enchants = null;
	
	public IKStack(ItemStack is) {
		itemid = is.getTypeId();
		damage = is.getDurability();
		quantity = is.getAmount();
		enchants = is.getEnchantments();
	}
	
	public IKStack(int id, int damage, int amount, Map<Enchantment, Integer> enchantments) {
		itemid = id;
		this.damage = damage;
		quantity = amount;
		enchants = enchantments;
	}
	
	public int getID() {
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
	
	public ItemStack[] getItemStacks() {
		if(quantity <= 64) {
			ItemStack[] is = {new ItemStack(itemid, quantity, (short)damage)};
			is[0].addUnsafeEnchantments(enchants);
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