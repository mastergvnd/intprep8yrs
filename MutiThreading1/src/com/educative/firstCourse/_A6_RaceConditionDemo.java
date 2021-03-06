package com.educative.firstCourse;

import java.util.Random;

public class _A6_RaceConditionDemo {

	public static void main(String[] args) throws InterruptedException {
		final RaceCondition rc = new RaceCondition();
		
		Thread t1 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				rc.printer();
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			
			@Override
			public void run() {
				rc.modifier();
			}
		}); 
		
		t1.start();
		t2.start();
		
		t1.join();
		t2.join();
		
		System.out.println("Completed task");
	}
}

class RaceCondition{
	int randomInt;
	Random random = new Random();
	
	void printer() {
		int i = 10000;
		while(i != 0) {
			if(randomInt % 5 == 0) {
				System.out.println("Number divisible by 5 : " + randomInt);
			}
			i--;
		}
	}
	
	void modifier() {
		int i = 10000;
		while(i != 0) {
			randomInt = random.nextInt(100);
			i--;
		}
	}
}