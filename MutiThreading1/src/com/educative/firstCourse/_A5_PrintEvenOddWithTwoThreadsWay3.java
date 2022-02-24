package com.educative.firstCourse;

import java.util.concurrent.atomic.AtomicInteger;

public class _A5_PrintEvenOddWithTwoThreadsWay3 {

	public static void main(String[] args) throws InterruptedException {
		
		final NumberPrinter2 numberPrinter = new NumberPrinter2();
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
		System.out.println("Main thread completed2.");
	}
}

class NumberPrinter2 {
	AtomicInteger number = new AtomicInteger(1);
	
	public void evenPrinter() {
		while(number.get() <= 20) {
			if(number.get() % 2 == 0) {
				System.out.println(Thread.currentThread().getName() + " : " + number);
				number.incrementAndGet();
			}
		}
	}
	
	public void oddPrinter() {
		while(number.get() <= 20) {
			if(number.get() % 2 != 0) {
				System.out.println(Thread.currentThread().getName() + " : " + number);
				number.incrementAndGet();
			}
		}
	}
}

//Please note this code prints number till 21. This is because the number is increased in one thread and the lock is being accessed by another thread. 
//The main reason is because the while loop is outside the synchronized block. 
