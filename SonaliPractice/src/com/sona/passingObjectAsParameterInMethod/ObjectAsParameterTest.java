package com.sona.passingObjectAsParameterInMethod;

public class ObjectAsParameterTest {

	public static void main(String[] args) {
		
		Employee e = new Employee();
		
		objectAsParameter(e);
		
		System.out.println(e.getId());
		System.out.println(e.getName());

	}

	private static void objectAsParameter(Employee emp) {
		emp.setId(5);
		emp.setName("Govind");
	}
}
