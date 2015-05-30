package com.tux2mc.infinitekits;

import java.util.UUID;

public class EditKit extends KitSet {
	
	public enum EditType {
		NONE,
		ADD,
		DELETE;
	}
	
	UUID player = null;
	IKStack currrentstack = null;
	EditType edittype = EditType.NONE;
	
	public EditKit(KitSet set, UUID player, String kitname) {
		super(kitname);
		//Make sure we aren't editing the original list!
		for(IKStack item : set.getKitItems()) {
			items.add(item);
		}
		cooldown = set.cooldown;
		cpprice = set.cpprice;
		icoprice = set.icoprice;
		needsboth = set.needsboth;
	}
	
	public EditKit(UUID player, String kitname) {
		super(kitname);
	}

	public UUID getPlayer() {
		return player;
	}

	public void setPlayer(UUID player) {
		this.player = player;
	}

	public IKStack getCurrrentStack() {
		return currrentstack;
	}

	public void setCurrrentStack(IKStack currrentstack) {
		this.currrentstack = currrentstack;
	}

	public EditType getEditType() {
		return edittype;
	}

	public void setEditType(EditType edittype) {
		this.edittype = edittype;
	}
}
