package com.govind.practice;

public class Child extends Parent {
	int var = 5;
	
	public void print() {
		System.out.println("Child");
		anotherCall();
	}
	public void anotherCall() {
		System.out.println("Another method from Child");
	}
}
