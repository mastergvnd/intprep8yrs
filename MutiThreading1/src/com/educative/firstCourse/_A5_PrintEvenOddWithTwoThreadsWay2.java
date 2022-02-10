package com.educative.firstCourse;

public class _A5_PrintEvenOddWithTwoThreadsWay2 {

	public static void main(String[] args) throws InterruptedException {
		
		final NumberPrinter numberPrinter = new NumberPrinter();
		Thread evenPrinter = new Thread(new Runnable() {
			@Override
			public void run() {
				numberPrinter.evenPrinter();
			}
		}, "Even Printer");
		
		Thread oddPrinter = new Thread(new Runnable() {
			@Override
			public void run() {
				numberPrinter.oddPrinter();
			}
		}, "Odd Printer");
		
		evenPrinter.start();
		oddPrinter.start();
		evenPrinter.join();
		oddPrinter.join();
		System.out.println("Main thread completed.");
	}
}

class NumberPrinter {
	int number = 1;
	
	public void evenPrinter() {
		while(number <= 20) {
			synchronized (this) {
				if(number % 2 == 0) {
					System.out.println(Thread.currentThread().getName() + " : " + number);
					number++;
				}
			}
		}
	}
	
	public void oddPrinter() {
		while(number <= 20) {
			synchronized (this) {
				if(number % 2 != 0) {
					System.out.println(Thread.currentThread().getName() + " : " + number);
					number++;
				}
			}
		}
	}
}

//Please note this code prints number till 21. This is because the number is increased in one thread and the lock is being accessed by another thread. 
//The main reason is because the while loop is outside the synchronized block. 
