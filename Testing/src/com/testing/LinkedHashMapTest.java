package com.testing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

public class LinkedHashMapTest {

	public static void main(String[] args) {
		LinkedHashMap<String, Map<String, String>> test = new LinkedHashMap<String, Map<String, String>>();
		
		HashMap<String, String> c1 = new HashMap<String, String>();
		c1.put("Banaras", "Bareilly");
		test.put("UP", c1);
		
		c1 = new HashMap<String, String>();
		c1.put("Bangalore", "Mangalore");
		test.put("Karnataka", c1);
		
		c1 = new HashMap<String, String>();
		c1.put("Patna", "Aara");
		test.put("Bihar", c1);
		
		System.out.println(test);
		
		System.out.println(ArrayUtils.indexOf(test.keySet().toArray(), "Karnataka"));
	
	}

}
