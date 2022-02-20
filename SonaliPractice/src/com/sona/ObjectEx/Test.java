package com.sona.ObjectEx;

public class Test {

	public static void main(String args[]) {

		StudentGenerator sg = new StudentGenerator();
		Student s1 = sg.generate();
        System.out.println(s1);
        System.out.println(s1.name);
        System.out.println(s1.rollno);
	}
}