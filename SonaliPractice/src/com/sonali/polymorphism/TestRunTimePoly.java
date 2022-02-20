package com.sonali.polymorphism;

public class TestRunTimePoly {

	public static void main(String[] args) {
		Animal a = new Animal();
		a.sound();

		Dog d = new Dog();
		d.sound();

		Cat c = new Cat();
		c.sound();
		
		a = new Dog();
		a.sound();
		
		a = new Cat();
		a.sound();
		
		a = c;
		
		a = d;

	}

}
