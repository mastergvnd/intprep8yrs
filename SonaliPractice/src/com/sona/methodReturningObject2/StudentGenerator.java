package com.sona.methodReturningObject2;

public class StudentGenerator {

	public Student generate(int a, String b){
		Student obj = new Student();
		obj.setRollNo(a);
		obj.setName(b);
		System.out.println("Memory address of obj : " + obj);
		return obj;
	}
	
}
