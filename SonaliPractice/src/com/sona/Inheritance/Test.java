package com.sona.Inheritance;

public class Test {

	public static void main(String[] args) {
		//Single Inheritance
		System.out.println("Single Inheritance");
		Kutiya malini = new Kutiya();
		malini.eating();
		malini.bitching();
		
		// Multiple Inheritance
		System.out.println("Multiple Inheritance");
		Puppy p = new Puppy();
		p.bitching();
		p.eating();
		p.add();
	}

}
