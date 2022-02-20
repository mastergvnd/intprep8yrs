package com.sona.Inheritance;

public class FourWheeler extends Vehicle {
	float b = speed + 7;

	public void measureFour() {
		System.out.println("Hi" + b);
		measure();
		measure(8, 9);

	}
}
