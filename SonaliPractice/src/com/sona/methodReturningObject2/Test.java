package com.sona.methodReturningObject2;

public class Test {
	public static void main(String args[]) {
		StudentGenerator sg = new StudentGenerator();
		
		Student s1 = sg.generate(11, "Sonali");
		System.out.println("Memory address of s1 : " + s1);
		System.out.println("New roll no:"+ s1.getRollNo());
		System.out.println("Name : " + s1.getName());
		
		Student s2 = sg.generate(22, "Govind");
		System.out.println("Memory address of s2 : " + s2);
		System.out.println("New roll no:"+ s2.getRollNo());
		System.out.println("Name : " + s2.getName());
		
		Student s3 = sg.generate(33, "Shivam");
		System.out.println("Memory address of s3 : " + s3);
		System.out.println("New roll no:"+ s3.getRollNo());
		System.out.println("Name : " + s3.getName());
		
		Student[] students = new Student[4];
		students[0] = s1;
		students[1] = s2;
		students[2] = s3;
		students[3] = null;
	}
}