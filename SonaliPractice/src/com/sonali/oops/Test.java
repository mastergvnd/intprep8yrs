package com.sonali.oops;

public class Test {

	public static void main(String[] args) {
		Employee sonali = new Employee();
		sonali.setId(12345);
		sonali.setName("SONU");
		sonali.setSalary(40000);
		
		System.out.println("Id : " + sonali.getId());
		System.out.println("Name : " + sonali.getName());
		System.out.println("Salary : " + sonali.getSalary());
		
		Employee govind = new Employee();
		govind.setId(1234);
		govind.setName("Gopu");
		govind.setSalary(34000);
		
		System.out.println("ID : " + govind.getId());
		System.out.println("name : " + govind.getName());
		System.out.println("salary : " + govind.getSalary());
	}

}