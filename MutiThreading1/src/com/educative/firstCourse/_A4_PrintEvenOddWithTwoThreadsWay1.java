package com.educative.firstCourse;

public class _A4_PrintEvenOddWithTwoThreadsWay1 {

	public static void main(String[] args) throws InterruptedException {
		
		final Number number = new Number();
		Thread evenPrinter = new Thread(new Runnable() {
			@Override
			public void run() {
				while(number.getValue() <= 20) {
					synchronized (this) {
						if(number.getValue() % 2 == 0) {
							System.out.println(Thread.currentThread().getName() + " : " + number.getValue());
							number.incrementValue();
						}
					}
				}
			}
		}, "Even Printer");
		
		Thread oddPrinter = new Thread(new Runnable() {
			@Override
			public void run() {
				while(number.getValue() <= 20) {
					synchronized (this) {
						if(number.getValue() % 2 != 0) {
							System.out.println(Thread.currentThread().getName() + " : " + number.getValue());
							number.incrementValue();
						}
					}
				}
			}
		}, "Odd Printer");
		
		evenPrinter.start();
		oddPrinter.start();
		evenPrinter.join();
		oddPrinter.join();
		System.out.println("Main thread completed.");

	}

}

class Number {
	int number = 1;
	
	public int getValue() {
		return number;
	}
	
	public void incrementValue() {
		number++;
	}
}
