package com.tux2mc.infinitekits;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;

public class IKBanner {
	
	DyeColor basecolor = DyeColor.BLACK;
	List<Pattern> patterns = null;
	
	public IKBanner(DyeColor basecolor, List<Pattern> patterns) {
		if(basecolor != null) {
			this.basecolor = basecolor;
		}
		if(patterns != null) {
			this.patterns = patterns;
		}else {
			this.patterns = new ArrayList<Pattern>();
		}
	}

	public DyeColor getBaseColor() {
		return basecolor;
	}

	public List<Pattern> getPatterns() {
		return patterns;
	}
}
