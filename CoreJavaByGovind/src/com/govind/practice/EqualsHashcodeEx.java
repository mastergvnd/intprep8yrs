package com.govind.practice;

import java.util.Collections;

public class EqualsHashcodeEx {

	public static void main(String[] args) {
		Person p1 = new Person(27, "Govind");
		Person p2 = new Person(27, "Govind");
		
		System.out.println(p1.equals(p2));
		System.out.println(p1 == p2);
		System.out.println(p1.hashCode() == p2.hashCode());
	}

}

class Person{
	int age;
	String name;
	
	Person(int age, String name) {
		this.age = age;
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * age;
		return result * name.hashCode(); 
	}
	
	@Override
	public boolean equals(Object o) {
		Person guest = (Person) o;
		return guest.age == this.age && guest.name.equals(this.name);
	}
}
