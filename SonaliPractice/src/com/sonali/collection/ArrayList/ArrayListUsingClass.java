package com.sonali.collection.ArrayList;

import java.util.ArrayList;

public class ArrayListUsingClass {

	public static void main(String args[]) {
		Employee e1 = new Employee();
		Employee e2 = new Employee();
		Employee e3 = new Employee(46,"Gopu");
		ArrayList<Employee> list = new ArrayList<Employee>();
		list.add(e1);
		list.add(e2);
		list.add(e3);
		System.out.println("Values of objects " + list);
	}
}
