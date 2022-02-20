package com.sonali.oops;

public class Employee {

	private int id;
	private String name;
	private float salary;

	public Employee() {
		
	}
	public void setId(int a) {
		this.id = a;
	}
	
	public int getId() {
		return this.id;
	}

    public void setName(String b) {
    	this.name = b;
    }
    
    public String getName() {
	   return this.name;
	}
    
    public void setSalary(float c) {
    	this.salary=c;
    }
    public float getSalary() {
    	return this.salary;
    }
}