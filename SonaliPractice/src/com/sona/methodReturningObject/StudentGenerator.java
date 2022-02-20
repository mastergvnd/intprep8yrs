package com.sona.methodReturningObject;

public class StudentGenerator {
	public Student generateStudent(int rollNo, String name, int standard) {
		Student obj = new Student();
		obj.setName("Sonali Varshney");
		obj.setRollno(1234);
		obj.setStandard(5);
		return obj;
	}
}
