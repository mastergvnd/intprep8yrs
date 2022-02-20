package com.sonali.polymorphism;

public class CompileTimePoly {
	public int add(int a, int b) {
		return a + b;
	}

	public double add(float a, int b, int c) {
		return a + b + c;
	}

	public double add(double a, double b) {
		return a + b;
	}
}
