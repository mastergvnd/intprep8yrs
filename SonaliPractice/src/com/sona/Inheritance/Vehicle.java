package com.sona.Inheritance;

public class Vehicle {
	float speed=3;

	public void measure() {

		int km = 50;
		int hr = 6;

		speed = km / hr;

		System.out.println(speed);

	}

	public void measure(int m, int sec) {
		speed = m / sec;
		System.out.println(speed);
	}

}
