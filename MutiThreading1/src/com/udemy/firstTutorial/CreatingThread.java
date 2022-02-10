package com.udemy.firstTutorial;

public class CreatingThread {

	public static void main(String[] args) {
		System.out.println("Current Thread : " + Thread.currentThread().getName());
		Thread w1 = new Thread(new Worker1());
		Thread w2 = new Thread(new Worker2());
		w1.start();
		w2.start();
		new Worker3().run();
	}

}


class Worker1 implements Runnable{
	public void run() {
		for(int i=0; i<5; i++)
			System.out.println("Worker 1 : " + i);
	}
}

class Worker2 implements Runnable{
	public void run() {
		for(int i=0; i<5; i++)
			System.out.println("Worker 2 : " + i);
	}
}

class Worker3 implements Runnable{
	public void run() {
		for(int i=0; i<5; i++)
			System.out.println("Worker 3 : " + i);
	}
}