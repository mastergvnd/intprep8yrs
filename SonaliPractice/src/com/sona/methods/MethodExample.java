package com.sona.methods;

public class MethodExample {

	public String getName() {
		return "Govind";
	}
	public static void main(String[] args) {
		MethodExample m = new MethodExample();
		String myName = m.getName();
		System.out.println(myName);

	}

}
