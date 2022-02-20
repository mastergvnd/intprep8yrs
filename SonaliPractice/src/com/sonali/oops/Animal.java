package com.sonali.oops;

public class Animal {
	public boolean vegetarian;
	public String eats;
	public int noOfLegs; 
	
	public Animal(boolean a, String b, int c) {
		this.vegetarian = a;
		this.eats = b;
		this.noOfLegs = c;
	}
	
	public Animal(){
		System.out.println("Animal");
	}
}
