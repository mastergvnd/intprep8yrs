package com.sna.collection.Linkedlist;

import java.util.Vector;

public class LinkedListClass {
	public static void main(String args[]) {
		Employee e1 = new Employee();
		Employee e2 = new Employee();
		Employee e3 = new Employee(46,"Gopu");
		Vector<Employee> list = new Vector<Employee>();
		list.add(e1);
		list.add(e2);
		list.add(e3);
		
		System.out.println("Values of objects " + list);
	}
	
}
