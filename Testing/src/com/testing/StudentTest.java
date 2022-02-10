package com.testing;

import java.util.ArrayList;

public class StudentTest {

	public static void main(String[] args) {
	
		ArrayList<Student> list = new ArrayList<Student>();
		list.add(new Student("Govind"));
		list.add(new Student("Gupta"));
		list.add(new Student("Sonali"));
		list.add(new Student("Varshney"));
		
		for(Student s : list) {
			System.out.println(s.getName() + " " + print(3, s));
		}
		
	}

	private static boolean print(int num, Student s) {
		return num==2 && s.equals(new Student("Gupta"));
		
	}

}
