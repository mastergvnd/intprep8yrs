package com.sonali.collection.ArrayList;

public class Employee {

	public int rollNo;
	public String name;

	public Employee() {
		System.out.println("The roll no is " + this.rollNo);
		System.out.println("The name is " + this.name);
	}
	
	public Employee(int a,String b){
		this.rollNo = a;
		this.name = b;
		System.out.println("The roll no for parameterized constructor is " + this.rollNo);
		System.out.println("The name for parameterized constructor is " + this.name);
	}
	
	//@Override
	//public String toString() {
		//return rollNo + " " + name;
	//}
}
