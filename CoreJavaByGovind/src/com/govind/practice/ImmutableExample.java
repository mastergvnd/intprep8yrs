package com.govind.practice;

import java.util.ArrayList;
import java.util.List;

public class ImmutableExample {

	public static void main(String[] args) {
		ArrayList<String> tasks = new ArrayList<String>();
		tasks.add("Stats");
		tasks.add("Email");
		
		HomeAddress address = new HomeAddress("Bangalore", 560068);
		
		Employee e1 = new Employee(1, "Govind", address, tasks);
		System.out.println(e1);
		
		tasks.add("Enh");
		address.setCity("Bareilly");
		
		e1.getTasks().add("Dashboard");
		e1.getAddress().setCity("Bareilly");
		
		System.out.println("Post change");
		System.out.println(e1);
		
		
	}
}

final class Employee{
	
	final private int empId;
	final private String name;
	final private HomeAddress address;
	final private ArrayList<String> tasks;

	public Employee(int rollNo, String name, HomeAddress address, ArrayList<String> tasks) {
		this.empId = rollNo;
		this.name = name;
		
		HomeAddress lAddress = new HomeAddress(address.getCity(), address.getZip());
		
		this.address = lAddress;
		
		ArrayList<String> lTasks = new ArrayList<String>();
		for(String task : tasks){
			lTasks.add(task);
		}
		
		this.tasks = lTasks;
	}
	
	public String toString() {
		return "{ " + empId + ", " + name + " ["+ address.getCity() + " " + address.getZip() + "]" +", [" + String.join(",", tasks) + "] }";
	}

	public int getEmpId() {
		return empId;
	}

	public String getName() {
		return name;
	}
	
	public HomeAddress getAddress() {
		return (HomeAddress) address.clone();
		//return address;
	}

	public List<String> getTasks() {
		return (List<String>) tasks.clone();
	}
}

final class HomeAddress implements Cloneable{
	private String city;
	private int zip;
	
	public HomeAddress(String city, int zip) {
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
	public HomeAddress clone() {
		return new HomeAddress (city, zip);
	}
}