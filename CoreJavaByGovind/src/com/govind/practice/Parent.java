package com.govind.practice;

public class Parent {
	int var = 9;
	
	public void print() {
		System.out.println("Parent");
		anotherCall();
		
	}
	public void anotherCall() {
		System.out.println("Another method from parent");
	}
}
