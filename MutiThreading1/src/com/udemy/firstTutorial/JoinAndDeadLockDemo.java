package com.udemy.firstTutorial;

public class JoinAndDeadLockDemo {

	public static void main(String[] args) throws InterruptedException {
		Worker4 w4 = new Worker4();
		w4.t = Thread.currentThread();
		Thread t1 = new Thread(w4);
		Thread t2 = new Thread(new Worker5());
		t1.start();
		//t1.join();
		t2.start();
		t1.join();
		t2.join();
		System.out.println("Supervisor : let me club them into final block.");
	}
}

class Worker4 implements Runnable{
	static Thread t;
	public void run() {
		try {
			t.join();
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Worker 1 cpompleted the job.");
	}
}

class Worker5 implements Runnable{
	public void run() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Worker 2 cpompleted the job.");
	}
}