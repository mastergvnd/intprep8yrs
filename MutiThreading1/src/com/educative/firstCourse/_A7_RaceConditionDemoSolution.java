package com.educative.firstCourse;

import java.util.Random;

public class _A7_RaceConditionDemoSolution {

	public static void main(String[] args) throws InterruptedException {
		final RaceCondition2 rc = new RaceCondition2();
		
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

class RaceCondition2{
	int randomInt;
	Random random = new Random();
	
	void printer() {
		int i = 1000;
		while(i != 0) {
			synchronized (this) {
				if(randomInt % 5 == 0) { //replace 5 with 2 to see the effect.
					System.out.println( i +" Number divisible by 5 : " + randomInt);
				}
			}
			i--;
		}
	}
	
	void modifier() {
		int i = 1000;
		while(i != 0) {
			synchronized (this) {
				randomInt = random.nextInt(100);
			}
			i--;
		}
	}
}