package com.hyperion.planning;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.essbase.api.base.EssException;

public class Outline {
	private HashMap<String, Dimension> dimensions = new LinkedHashMap<String, Dimension>();

	public HashMap<String, Dimension> getDimensions() {
		return dimensions;
	}
	public void setDimensions(HashMap<String, Dimension> dimensions) {
		this.dimensions = dimensions;
	}
	public void add(Dimension dimension) throws EssException{
		dimensions.put(dimension.getDimension().getName(), dimension);
	}
}
