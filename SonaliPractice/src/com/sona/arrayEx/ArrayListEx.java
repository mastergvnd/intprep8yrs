package com.sona.arrayEx;

import java.util.ArrayList;

public class ArrayListEx {
	public static void main(String[] args) {
		ArrayList list = new ArrayList();
		list.add(99);
		list.add("Govind");
		list.add(true);
		list.add(5.5);
		
		System.out.println(list.get(1));
		list.add(2, "Sonali");
		System.out.println(list);
		System.out.println(list.contains("Govinda"));
		System.out.println(list.indexOf("Govind"));
		list.remove(1);
		list.remove(true);
		System.out.println(list.size());
		System.out.println(list);
		
		for(Object o : list) {
			System.out.println(o);
		}
		
		ArrayList p = (ArrayList)getList();
		System.out.println(p);
		p = (ArrayList)getList2();
		System.out.println(p);
	}
	
	public static ArrayList getList(){
		ArrayList a = new ArrayList();
		a.add("Govind");
		return a;
	}
	
	public static Object getList2(){
		return new Object();
	}
}
