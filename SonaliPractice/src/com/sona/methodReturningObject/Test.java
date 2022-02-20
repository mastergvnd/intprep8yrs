package com.sona.methodReturningObject;

public class Test {

	public static void main(String[] args) {
		StudentGenerator a = new StudentGenerator();

		Student student = a.generateStudent(1, "eed", 2);
		System.out.println(student.getRollno());
		System.out.println(student.getName());
		System.out.println(student.getStandard());

	}

}
