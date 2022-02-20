package com.sonali.collection.Hashmap;

import java.util.HashMap;

public class HashMapClass {
	public static void main(String[] args) {
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("Husband", "Gopu");
		hm.put("Wife", "Sonali");
		System.out.println("The list is: " + hm);

		boolean a = hm.containsKey("wife");
		System.out.println("Wife key is there or not? " + a);

		boolean b = hm.containsValue("Sonali");
		System.out.println("Sonali value is there or not? " + b);

		// hm.clone();

		String c = hm.get("Wife");
		System.out.println("Get Wife key ? " + c);

		String d = hm.getOrDefault("Wife", "Sonali");
		System.out.println("Get Or Default? " + d);

		boolean e = hm.isEmpty();
		System.out.println("Is Empty? " +e);
		
		hm.putIfAbsent("Mother", "Child");
		System.out.println("The list is: " + hm);
		
		hm.putIfAbsent("Wife", "Sonali");
		System.out.println("The list is: " + hm);
		
		hm.remove("Mother");
		System.out.println("After removal of Mother key, list is: "+hm);
		
		hm.remove("Wife", "Sonali");
		System.out.println("After removal of Mother key and Sonali value, list is: "+hm);
		
		int size = hm.size();
		System.out.println("The size is "+size);
		
		hm.replace("Husband", "Gopu", "Govind");
		System.out.println("The list is: " + hm);
		
		hm.replace("Husband", "Golu");
		System.out.println("The list is: " + hm);
		
	}
}
