package com.sona.collection.Hashset;

import java.util.HashMap;
import java.util.HashSet;

public class NormalTest {

	public static void main(String[] args) {
		HashSet<String> set = new HashSet<String>();
		set.add("Govind");
		set.add("Sonali");
		set.add("Govind"); //it will exclude it bcz it is unique set
		set.add("Baby");
		
		System.out.println("List for set is: " +set);
		
		HashSet<String> set1 = new HashSet<String>();
		set1.add("Govind");
		set1.add("Sonali");
		set1.add("Sona");
		set1.add("Gopu");
		
		System.out.println("List for set1 is: " +set1);
		
		boolean a = set1.containsAll(set);
		System.out.println("set1 contains set?"+a);
	
		boolean e = set1.contains("Sonali");
		System.out.println("Contains??" +e);
		
		set1.addAll(set);
		System.out.println("List for set is: " +set);
		System.out.println("List for set1 is: " +set1);
		
		set1.size();
		System.out.println("The size of set1 is :" +set1.size());
		
		set1.remove("Gopu");
		System.out.println("List for set is: " +set);
		System.out.println("List for set1 is: " +set1);
		
		set1.getClass();
		System.out.println("Get class for set1 is: " +set1.getClass());
		
		boolean b = set1.isEmpty();
		System.out.println("Is empty? " +b);
		
		set1.retainAll(set);
		System.out.println("List for set is: " +set);
		System.out.println("List for set1 is: " +set1);
		
		boolean c = set1.equals("Sonali");
		System.out.println("Is equal? "+c);
		
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("Husband", "Wife");
		map.put("hubby","wiffy");
	
		//boolean d = set1.'containsAll(map);//can't compare hashmap with hashset . same type of collection only can be compared.
		//System.out.println("Contains all? "+d);
		
	
	}

}
