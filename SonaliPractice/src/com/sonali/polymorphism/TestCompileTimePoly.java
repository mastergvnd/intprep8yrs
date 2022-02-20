package com.sonali.polymorphism;

public class TestCompileTimePoly {

	public static void main(String[] args) {
		CompileTimePoly obj = new CompileTimePoly();

		int result = obj.add(1, 2);
		
		double result2 = obj.add(3.1, 3.2);
		System.out.println(result);
	}

}
