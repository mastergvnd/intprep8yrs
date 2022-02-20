package com.sonali.oops;

public class Dog extends Animal{
	int a;
	public Dog() {
		super();
		System.out.println("dog");
	}
	
	public Dog(int a) {
		super();
		this.a = a;
	}
	
	public static void main(String[] args) {
		Dog d = new Dog();
	}
}
