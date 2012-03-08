package com.tux2mc.infinitekits;

import java.util.LinkedList;

import org.bukkit.inventory.ItemStack;

public class KitSet {
	
	LinkedList<IKStack> items = new LinkedList<IKStack>();
	double icoprice = 0;
	int cpprice = 0;
	boolean needsboth = true;
	
	public KitSet() {
	}
	
	public KitSet(LinkedList<IKStack> items) {
		this.items = items;
	}
	
	public LinkedList<IKStack> getKit() {
		return items;
	}
	
	public void clearKit() {
		items.clear();
	}
	
	public double getKitPrice() {
		return icoprice;
	}
	
	public int getCpPrice() {
		return cpprice;
	}
	
	public void setKitPrice(double price) {
		icoprice = price;
	}
	
	public void setCpPrice(int price) {
		cpprice = price;
	}
	
	public boolean needBothCPandIco() {
		return needsboth;
	}
	
	public void setNeedsBoth(boolean needs) {
		needsboth = needs;
	}
	
	public LinkedList<IKStack> getKitItems() {
		return items;
	}
	
	public void addItem(ItemStack is) {
		items.add(new IKStack(is));
	}
	
	public void addItem(IKStack is) {
		items.add(is);
	}
	
	public void removeItem(ItemStack is) {
		items.remove(new IKStack(is));
	}
	
	public void removeItem(IKStack is) {
		items.remove(is);
	}
	
	public ItemStack[] getKitStacks() {
		int totalitems = 0;
		for(int i = 0; i < items.size(); i++) {
			totalitems += items.get(i).getNumberOfStacks();
		}
		ItemStack[] isitems = new ItemStack[totalitems];
		int itemstackint = 0;
		int itemsint = 0;
		while(itemstackint < isitems.length && itemsint < items.size()) {
			ItemStack[] tempstack = items.get(itemsint).getItemStacks();
			for(int i = 0; i < tempstack.length && itemstackint < isitems.length; i++) {
				isitems[itemstackint] = tempstack[i];
				itemstackint++;
			}
			itemsint++;
		}
		return isitems;
	}

}