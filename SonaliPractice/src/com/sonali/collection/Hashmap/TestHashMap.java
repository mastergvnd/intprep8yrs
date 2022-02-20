package com.sonali.collection.Hashmap;

import java.util.HashMap;

public class TestHashMap {
	public static void main(String[] args) {
		
		Employee e1 = new Employee();
		Employee e2 = new Employee();
		
		e1.setId(12);
		e1.setName("Sona");
		
		e2.setId(34);
		e2.setName("Gopu");
		
		HashMap<String,Employee> hm = new HashMap<String,Employee>();
		hm.put("Sonali", e1);
		hm.put("Govind", e2);
		
		System.out.println("List is "+hm);
		
		System.out.println("Key Set is : "+ hm.keySet());
		
		for(String key : hm.keySet()) {
			Employee tmp = hm.get(key);
			System.out.println("Key is : " + key);
			System.out.println("Id is : " + tmp.getId());
			System.out.println("Name is : " + tmp.getName());
			System.out.println();
		}
		
	}

}
