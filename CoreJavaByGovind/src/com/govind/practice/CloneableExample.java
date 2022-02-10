package com.govind.practice;

import java.util.ArrayList;
import java.util.List;

public class CloneableExample {

	public static void main(String[] args) {
		ArrayList<String> subjects = new ArrayList<String>();
		subjects.add("Computer Science");
		subjects.add("Mathematics");
		subjects.add("Data Structures");
		
		Address address = new Address("Bangalore", 560068);
		
		Student s1 = new Student(1, "Govind", address, subjects);
		System.out.println("       Student 1 : " + s1);
		Student s2 = s1.clone();
		System.out.println("cloned Student 2 : " + s2);
		s1.setName("Govind Gupta");
		subjects.add("Java");
		address.setCity("Bareilly");
		
		System.out.println("---------------------------------------------------------------------------------------------");
		System.out.println("After change in object1");
		System.out.println("       Student 1 : " + s1);
		System.out.println("Cloned Student 2 : " + s2);
	}
}


class Student implements Cloneable{
	
	int rollNo;
	String name;
	Address address;
	ArrayList<String> subjects;

	public Student(int rollNo, String name, Address address, ArrayList<String> subjects) {
		this.rollNo = rollNo;
		this.name = name;
		this.address = address;
		this.subjects = subjects;
	}
	
	protected Student clone() {
		Student clonedOnj = new Student(this.rollNo, this.name, address.clone(),(ArrayList<String>) this.subjects.clone());
		return clonedOnj;
	}
	
	public String toString() {
		return "{ " + rollNo + ", " + name + " ["+ address.getCity() + " " + address.getZip() + "]" +", [" + String.join(",", subjects) + "] }";
	}

	public int getRollNo() {
		return rollNo;
	}

	public String getName() {
		return name;
	}

	public List<String> getSubjects() {
		return subjects;
	}

	public void setRollNo(int rollNo) {
		this.rollNo = rollNo;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSubjects(ArrayList<String> subjects) {
		this.subjects = subjects;
	}
}

class Address implements Cloneable{
	private String city;
	private int zip;
	
	public Address(String city, int zip) {
		this.city = city;
		this.zip= zip;
	}
	public String getCity() {
		return city;
	}
	public int getZip() {
		return zip;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public void setZip(int zip) {
		this.zip = zip;
	}	
	
	public Address clone() {
		return new Address (city, zip);
	}
}