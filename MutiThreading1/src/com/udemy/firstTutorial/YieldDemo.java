package com.udemy.firstTutorial;

public class YieldDemo {

	public static void main(String[] args) {
		Thread t = new Thread(new MyRunnable());
		t.start();
		for (int i = 0; i < 10; i++) {
			System.out.println("Main Thread : " + i);
		}
	}

}


class MyRunnable implements Runnable {
	public void run() {
		for (int i = 0; i < 10; i++) {
			System.out.println("Child Thread : " + i);
			Thread.yield();
		}
	}
}