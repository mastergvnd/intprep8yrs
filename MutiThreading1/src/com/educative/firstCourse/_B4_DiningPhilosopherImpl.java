package com.educative.firstCourse;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class _B4_DiningPhilosopherImpl {

	public static void main(String[] args) throws InterruptedException {
		DiningPhilosophers dp = new DiningPhilosophers();
		Thread p1 = new Thread(() -> {
			startPhilosopher(dp, 0);
		});
		
		Thread p2 = new Thread(() -> {
			startPhilosopher(dp, 1);
		});
		
		Thread p3 = new Thread(() -> {
			startPhilosopher(dp, 2);
		});
		
		Thread p4 = new Thread(() -> {
			startPhilosopher(dp, 3);
		});
		
		Thread p5 = new Thread(() -> {
			startPhilosopher(dp, 4);
		});
		
		p1.start();
		p2.start();
		p3.start();
		p4.start();
		p5.start();
		
		p1.join();
		p2.join();
		p3.join();
		p4.join();
		p5.join();
		System.out.println("Dining complated!!");
	}
	
	private static void startPhilosopher(DiningPhilosophers dp, int id) {
		try {
			dp.lifeCycleOfaPhilosopher(id);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}


class DiningPhilosophers{
	Semaphore forks[] = new Semaphore[5];
	Semaphore maxDiners = new Semaphore(4);
	Random random = new Random();
	
	public DiningPhilosophers() {
		forks[0] = new Semaphore(1);
		forks[1] = new Semaphore(1);
		forks[2] = new Semaphore(1);
		forks[3] = new Semaphore(1);
		forks[4] = new Semaphore(1);
	}
	
	public void lifeCycleOfaPhilosopher(int id) throws InterruptedException {
		while(true) {
			think(id);
			eat(id);
		}
	}

	private void think(int id) throws InterruptedException {
		System.out.println("Philosopher " + id + "is thinking...");
		Thread.sleep(random.nextInt(500));
	}
	
	private void eat(int id) throws InterruptedException {
		maxDiners.acquire();
		
		forks[id].acquire();
		forks[(id + 1) % 5].acquire();
		
		System.out.println("Philosopher " + id + "is eating.");
		
		forks[id].release();
		forks[(id + 1) % 5].release();
		
		maxDiners.release();
	}
}